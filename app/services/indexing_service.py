from typing import List, Optional
from llama_index.core import Document, VectorStoreIndex, Settings as LlamaSettings
from llama_index.core.embeddings import resolve_embed_model  # Correct import path
from llama_index.llms.groq import Groq as LlamaGroq  # LLM for LlamaIndex if needed for synthesis/ranking

from app.models import SimpleInsightInfo, NextInsightResponse
from app.core.config import settings

# Configure LlamaIndex settings
# You can use a local embedding model or an API-based one.
# For local, HuggingFaceEmbeddings is good. For API, OpenAI or Cohere.
# Let's use a default local one if not specified, or configure for Groq if preferred.
# LlamaSettings.embed_model = resolve_embed_model("local:BAAI/bge-small-en-v1.5") # Fast local model
# If you want to use an API for embeddings (e.g. OpenAI, Cohere, or even Groq if they offer one):
# LlamaSettings.embed_model = OpenAIEmbedding() # Requires OPENAI_API_KEY
try:
    LlamaSettings.embed_model = resolve_embed_model("local:BAAI/bge-small-en-v1.5")
    print("LlamaIndex: Using local HuggingFace embeddings (BAAI/bge-small-en-v1.5).")
except Exception as e:
    print(f"Warning: Could not load local embedding model BAAI/bge-small-en-v1.5: {e}. Falling back to default.")
    # Fallback or configure another, e.g., if using an API service for embeddings.
    # For now, if local fails, some LlamaIndex operations requiring embeddings might be impaired.


# Optionally configure an LLM for LlamaIndex (e.g., for query synthesis or advanced ranking)
# LlamaSettings.llm = LlamaGroq(model="mixtral-8x7b-32768", api_key=settings.GROQ_API_KEY)
# print("LlamaIndex: Using Groq LLM (mixtral-8x7b-32768) for synthesis/ranking.")


def choose_next_insight_logic(uncompleted_insights: List[SimpleInsightInfo],
                              user_id: Optional[int]) -> NextInsightResponse:
    print(f"--- Choosing Next Insight for User {user_id} from {len(uncompleted_insights)} options ---")
    if not uncompleted_insights:
        # This case should ideally be handled by the Java backend before calling Python
        raise ValueError("No uncompleted insights provided to choose from.")

    # Basic Fallback: If LlamaIndex has issues or for simplicity, pick the one shown least, then by score.
    # sorted_insights = sorted(uncompleted_insights, key=lambda x: (x.times_shown or 0, -(x.relevance_score or 0.0)))
    # chosen_insight_id = sorted_insights[0].insight_id
    # print(f"Fallback choice: Insight ID {chosen_insight_id}")
    # return NextInsightResponse(insight_id=chosen_insight_id)

    # LlamaIndex approach:
    # 1. Create Document objects from the uncompleted insights.
    # 2. Build an in-memory vector index.
    # 3. Formulate a query. The query could be generic ("next best learning unit") or
    #    incorporate user performance if available and relevant (e.g., "a unit related to areas of user weakness").
    #    For now, a generic query.

    documents = []
    for insight_info in uncompleted_insights:
        # We can add more metadata for LlamaIndex to use during retrieval
        doc_metadata = {
            "insight_id": insight_info.insight_id,
            "times_shown": insight_info.times_shown or 0,
            "relevance_score": insight_info.relevance_score or 0.0,
            # "user_id": user_id # if user-specific context is embedded
        }
        # The content of the document is the insight title, could also include a summary if available
        documents.append(Document(text=insight_info.title, metadata=doc_metadata))

    if not documents:  # Should be caught by the earlier check
        raise ValueError("No documents created from uncompleted insights.")

    try:
        # Build index in memory for each request (can be slow for many insights, but suitable for ~30)
        # For larger scale, a persistent VectorStore would be better.
        index = VectorStoreIndex.from_documents(documents)  # This uses LlamaSettings.embed_model

        # Create a query. For now, let's assume a general query to find a relevant next step.
        # More sophisticated queries could use user's past performance or preferences.
        # A simple query might be just the topic name or a generic "next learning objective".
        # Here, we're just trying to get LlamaIndex to rank/retrieve from the small set.
        # A simple query could be: "What is the most relevant next insight to learn?"
        # Or we can use a Retriever with similarity_top_k=1 and custom filtering/sorting.

        query_str = "What is a good next insight to learn to improve understanding?"  # Generic query

        # Using a retriever to get more control
        retriever = index.as_retriever(similarity_top_k=len(documents))  # Get all, then sort
        retrieved_nodes = retriever.retrieve(query_str)

        if not retrieved_nodes:
            print("LlamaIndex retriever returned no nodes. Falling back to first insight.")
            return NextInsightResponse(insight_id=uncompleted_insights[0].insight_id)

        # Now, apply custom sorting/logic based on metadata
        # For example, prioritize insights shown fewer times, then by relevance score from retrieval

        # Create a dictionary to map insight_id to its retrieval score
        node_scores = {node.metadata["insight_id"]: node.get_score() for node in retrieved_nodes if
                       "insight_id" in node.metadata}

        # Sort the original uncompleted_insights list based on our criteria:
        # 1. Times shown (ascending)
        # 2. LlamaIndex retrieval score (descending, if available)
        # 3. Original relevance_score (descending, as a tie-breaker)
        def sort_key(insight: SimpleInsightInfo):
            retrieval_score = node_scores.get(insight.insight_id, 0.0)  # Default to 0 if not scored
            return (
                insight.times_shown or 0,  # Less is better
                -retrieval_score,  # More is better
                -(insight.relevance_score or 0.0)  # More is better
            )

        sorted_insights = sorted(uncompleted_insights, key=sort_key)

        if not sorted_insights:  # Should not happen if uncompleted_insights is not empty
            print("Sorting resulted in empty list. Fallback.")
            return NextInsightResponse(insight_id=uncompleted_insights[0].insight_id)

        chosen_insight = sorted_insights[0]
        print(
            f"LlamaIndex based choice (after custom sort): Insight ID {chosen_insight.insight_id}, Title: '{chosen_insight.title}', Times shown: {chosen_insight.times_shown}, Retrieval Score: {node_scores.get(chosen_insight.insight_id, 'N/A')}")
        return NextInsightResponse(insight_id=chosen_insight.insight_id)

    except Exception as e:
        print(f"Error during LlamaIndex processing: {e}. Falling back to simpler logic.")
        # Fallback to simpler logic (e.g., least shown, then by original relevance score)
        sorted_insights_fallback = sorted(uncompleted_insights,
                                          key=lambda x: (x.times_shown or 0, -(x.relevance_score or 0.0)))
        if not sorted_insights_fallback:
            raise ValueError("Fallback sorting also failed or list is empty.")  # Should be caught earlier
        chosen_insight_id_fallback = sorted_insights_fallback[0].insight_id
        print(f"Fallback choice (due to LlamaIndex error): Insight ID {chosen_insight_id_fallback}")
        return NextInsightResponse(insight_id=chosen_insight_id_fallback)