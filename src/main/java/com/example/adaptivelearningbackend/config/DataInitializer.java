package com.example.adaptivelearningbackend.config;

import com.example.adaptivelearningbackend.entity.AssessmentQuestionEntity;
import com.example.adaptivelearningbackend.entity.DomainEntity;
import com.example.adaptivelearningbackend.entity.RoleEntity;
import com.example.adaptivelearningbackend.entity.UserEntity;
import com.example.adaptivelearningbackend.repository.DomainRepository;
import com.example.adaptivelearningbackend.repository.RoleRepository;
import com.example.adaptivelearningbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final DomainRepository domainRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    private record QuestionTemplate(String text, List<String> options) {}


    private static final List<QuestionTemplate> GENERIC_QUESTION_TEMPLATES = List.of(
            new QuestionTemplate(
                    "What primarily motivates you to learn about [Domain]?",
                    List.of("Career advancement", "Personal hobby", "Academic requirement", "A project need", "Keeping up with trends", "Just exploring")
            ),
            new QuestionTemplate(
                    "How would you describe your current understanding of [Domain]?",
                    List.of("Absolute beginner", "Beginner", "Intermediate", "Advanced", "Expert")
            ),
            new QuestionTemplate(
                    "What is your primary goal for learning [Domain]?",
                    List.of("Grasp the basics", "Master advanced topics", "Prepare for certification/exam", "Build something specific", "Use at work", "Explore interest")
            ),
            new QuestionTemplate(
                    "How do you prefer to learn new topics like [Domain]?",
                    List.of("Quick overview then practice", "Theory first", "Real-world examples", "Short, frequent sessions", "Longer, focused blocks")
            )
    );

    private static final Map<String, List<QuestionTemplate>> CONCEPT_QUESTION_TEMPLATES = Map.ofEntries(
            // Technology & Computer Science
            Map.entry("Python Programming", List.of(
                    new QuestionTemplate(
                            "What does the `def` keyword introduce in Python?",
                            List.of("A function definition", "A loop", "A class", "A comment", "I’m not sure")
                    )
            )),
            Map.entry("Web Development - Front-end", List.of(
                    new QuestionTemplate(
                            "Which language primarily handles the structure of a web page?",
                            List.of("HTML", "CSS", "JavaScript", "SQL", "Not sure")
                    )
            )),
            Map.entry("Web Development - Back-end", List.of(
                    new QuestionTemplate(
                            "In RESTful design, which HTTP verb is normally used to retrieve data?",
                            List.of("GET", "POST", "PUT", "DELETE", "Not sure")
                    )
            )),
            Map.entry("Data Science & Machine Learning", List.of(
                    new QuestionTemplate(
                            "Which step usually comes first in a data-science workflow?",
                            List.of("Data cleaning", "Model training", "Feature scaling", "Hyper-parameter tuning", "Not sure")
                    )
            )),
            Map.entry("Artificial Intelligence Fundamentals", List.of(
                    new QuestionTemplate(
                            "Turing Test is primarily used to assess…?",
                            List.of("Machine intelligence", "Network speed", "Processor heat", "Data privacy", "Not sure")
                    )
            )),
            Map.entry("Cybersecurity Basics", List.of(
                    new QuestionTemplate(
                            "Phishing attacks mainly attempt to obtain…?",
                            List.of("User credentials", "Physical hardware", "Network bandwidth", "Encryption keys only", "Not sure")
                    )
            )),
            Map.entry("Cloud Computing (AWS/Azure/GCP)", List.of(
                    new QuestionTemplate(
                            "IaaS primarily provides which of the following?",
                            List.of("Virtual servers & storage", "Managed databases", "Email hosting", "SaaS applications", "Not sure")
                    )
            )),
            Map.entry("Mobile App Development (iOS/Android)", List.of(
                    new QuestionTemplate(
                            "Android apps are most commonly written in…?",
                            List.of("Kotlin/Java", "Swift", "JavaScript only", "C# only", "Not sure")
                    )
            )),
            Map.entry("Game Development (Unity/Unreal)", List.of(
                    new QuestionTemplate(
                            "In Unity, scenes are used to…?",
                            List.of("Organise game levels", "Store audio files", "Write shaders", "Manage version control", "Not sure")
                    )
            )),
            Map.entry("Database Management (SQL & NoSQL)", List.of(
                    new QuestionTemplate(
                            "JOIN in SQL is mainly for…?",
                            List.of("Combining rows from tables", "Encrypting data", "Creating indices", "Backing up DB", "Not sure")
                    )
            )),
            Map.entry("DevOps Practices & Tools", List.of(
                    new QuestionTemplate(
                            "Docker is fundamentally a tool for…?",
                            List.of("Containerisation", "Monitoring", "Version control", "Virtual private networking", "Not sure")
                    )
            )),
            Map.entry("Blockchain & Cryptocurrencies", List.of(
                    new QuestionTemplate(
                            "What is ‘mining’ in the context of Bitcoin?",
                            List.of("Validating transactions & adding blocks", "Stealing coins", "Encrypting wallets", "Buying tokens", "Not sure")
                    )
            )),
            Map.entry("UI/UX Design Principles", List.of(
                    new QuestionTemplate(
                            "A ‘persona’ in UX design represents…?",
                            List.of("A fictional target user", "A colour palette", "A layout grid", "A design tool", "Not sure")
                    )
            )),
            Map.entry("Quantum Computing Concepts", List.of(
                    new QuestionTemplate(
                            "A qubit differs from a classical bit because it can…?",
                            List.of("Exist in superposition", "Store more voltage", "Hold bigger integers", "Never lose data", "Not sure")
                    )
            )),
            Map.entry("Internet of Things (IoT) Fundamentals", List.of(
                    new QuestionTemplate(
                            "MQTT in IoT is a…?",
                            List.of("Lightweight messaging protocol", "Hardware sensor", "Cloud platform", "Security algorithm", "Not sure")
                    )
            )),
            // Business & Finance
            Map.entry("Personal Finance & Investing", List.of(
                    new QuestionTemplate(
                            "Diversification means…?",
                            List.of("Spreading investments to reduce risk", "Putting all money in one asset", "Govt bonds only", "Short-term gains focus", "Not sure")
                    )
            )),
            Map.entry("Digital Marketing & SEO", List.of(
                    new QuestionTemplate(
                            "Which factor directly influences organic search ranking?",
                            List.of("Backlink quality", "TV ads", "Page font choice", "Office location", "Not sure")
                    )
            )),
            Map.entry("Entrepreneurship & Startup Building", List.of(
                    new QuestionTemplate(
                            "A minimum viable product is…?",
                            List.of("Simplest version to test with users", "Full-featured product launch", "Marketing slogan", "Investor term for valuation", "Not sure")
                    )
            )),
            Map.entry("Project Management (Agile & Waterfall)", List.of(
                    new QuestionTemplate(
                            "In Scrum, a fixed-length work cycle is called…?",
                            List.of("Sprint", "Milestone", "Gantt", "Epic", "Not sure")
                    )
            )),
            Map.entry("Business Analytics", List.of(
                    new QuestionTemplate(
                            "A KPI is best described as…?",
                            List.of("Key performance indicator", "Knowledge process input", "Keyboard protocol interface", "Customer persona", "Not sure")
                    )
            )),
            Map.entry("Supply Chain Management", List.of(
                    new QuestionTemplate(
                            "Just-in-time (JIT) aims to…?",
                            List.of("Minimise inventory stock", "Increase warehouse space", "Slow delivery", "Raise safety stock", "Not sure")
                    )
            )),
            Map.entry("E-commerce Strategies", List.of(
                    new QuestionTemplate(
                            "In e-commerce, a ‘conversion’ typically means…?",
                            List.of("Visitor completes purchase", "Page view", "Newsletter read", "Inventory update", "Not sure")
                    )
            )),
            Map.entry("Stock Market Trading Strategies", List.of(
                    new QuestionTemplate(
                            "A stop-loss order is used to…?",
                            List.of("Limit potential losses", "Increase leverage", "Collect dividends", "Pay brokerage fees", "Not sure")
                    )
            )),
            Map.entry("Accounting Fundamentals", List.of(
                    new QuestionTemplate(
                            "The accounting equation is…?",
                            List.of("Assets = Liabilities + Equity", "Revenue – Expenses = Profit", "Cash = Assets – Liabilities", "Not sure")
                    )
            )),
            Map.entry("Negotiation Skills", List.of(
                    new QuestionTemplate(
                            "BATNA stands for…?",
                            List.of("Best alternative to a negotiated agreement", "Basic attention to needs & aims", "Budget allocation & tally note", "Not sure")
                    )
            )),
            // Arts & Humanities
            Map.entry("Creative Writing", List.of(
                    new QuestionTemplate(
                            "The plot of a story is…?",
                            List.of("Sequence of events", "Main character", "Theme", "Setting", "Not sure")
                    )
            )),
            Map.entry("Digital Photography", List.of(
                    new QuestionTemplate(
                            "ISO on a camera controls…?",
                            List.of("Sensor sensitivity", "Shutter speed", "Aperture size", "White balance only", "Not sure")
                    )
            )),
            Map.entry("Music Theory & Composition", List.of(
                    new QuestionTemplate(
                            "A major triad consists of…?",
                            List.of("Root, major third, perfect fifth", "Three minor thirds", "Two whole tones", "Not sure")
                    )
            )),
            Map.entry("Graphic Design (Adobe Suite)", List.of(
                    new QuestionTemplate(
                            "Vector graphics are best for…?",
                            List.of("Scalability without pixelation", "Photograph editing only", "Raster images", "Not sure")
                    )
            )),
            Map.entry("History (Topic-driven)", List.of(
                    new QuestionTemplate(
                            "The Treaty of Versailles formally ended which war?",
                            List.of("World War I", "World War II", "Crimean War", "Not sure")
                    )
            )),
            Map.entry("Philosophy", List.of(
                    new QuestionTemplate(
                            "Aristotle’s Nicomachean Ethics focuses on…?",
                            List.of("Virtue ethics", "Utilitarianism", "Skepticism", "Not sure")
                    )
            )),
            Map.entry("Learning a Musical Instrument", List.of(
                    new QuestionTemplate(
                            "Which hand typically plays the melody on a piano?",
                            List.of("Right hand (treble)", "Left hand", "Both identical", "Not sure")
                    )
            )),
            Map.entry("Drawing & Illustration", List.of(
                    new QuestionTemplate(
                            "One-point perspective uses…?",
                            List.of("A single vanishing point", "Two vanishing points", "No horizon line", "Not sure")
                    )
            )),
            Map.entry("Film Making Basics", List.of(
                    new QuestionTemplate(
                            "The 180-degree rule helps maintain…?",
                            List.of("Spatial continuity", "Exposure", "Sound quality", "Not sure")
                    )
            )),
            Map.entry("Journalism & Media Ethics", List.of(
                    new QuestionTemplate(
                            "Plagiarism in journalism refers to…?",
                            List.of("Using others’ work without attribution", "Misspelling names", "Asking tough questions", "Not sure")
                    )
            )),
            // Science & Mathematics
            Map.entry("Physics Fundamentals", List.of(
                    new QuestionTemplate(
                            "Newton’s second law states…?",
                            List.of("F = m·a", "For every action…", "Energy cannot be created…", "Not sure")
                    )
            )),
            Map.entry("Chemistry Basics", List.of(
                    new QuestionTemplate(
                            "The atomic number equals the number of…?",
                            List.of("Protons", "Neutrons", "Electrons in outer shell only", "Not sure")
                    )
            )),
            Map.entry("Biology", List.of(
                    new QuestionTemplate(
                            "Photosynthesis converts light energy into…?",
                            List.of("Chemical energy (glucose)", "Kinetic energy", "Sound energy", "Not sure")
                    )
            )),
            Map.entry("Mathematics (Calculus, Linear Alg.)", List.of(
                    new QuestionTemplate(
                            "The derivative of a function measures…?",
                            List.of("Instantaneous rate of change", "Area under curve", "Volume", "Not sure")
                    )
            )),
            Map.entry("Astronomy & Astrophysics", List.of(
                    new QuestionTemplate(
                            "A light-year is a measure of…?",
                            List.of("Distance", "Time", "Luminosity", "Not sure")
                    )
            )),
            Map.entry("Environmental Science", List.of(
                    new QuestionTemplate(
                            "The greenhouse effect describes…?",
                            List.of("Atmospheric trapping of heat", "Hole in ozone", "Acid rain", "Not sure")
                    )
            )),
            Map.entry("Neuroscience Basics", List.of(
                    new QuestionTemplate(
                            "Neurons communicate across gaps called…?",
                            List.of("Synapses", "Axons", "Dendrites", "Not sure")
                    )
            )),
            Map.entry("Genetics & Genomics", List.of(
                    new QuestionTemplate(
                            "CRISPR technology enables…?",
                            List.of("Targeted gene editing", "Protein folding", "X-ray imaging", "Not sure")
                    )
            )),
            Map.entry("Geology & Earth Science", List.of(
                    new QuestionTemplate(
                            "The Mohs scale measures…?",
                            List.of("Mineral hardness", "Rock age", "Earthquake intensity", "Not sure")
                    )
            )),
            // Languages
            Map.entry("Learn Spanish", List.of(
                    new QuestionTemplate(
                            "In Spanish, nouns that end in ‘-o’ are usually…?",
                            List.of("Masculine", "Feminine", "Neutral", "Not sure")
                    )
            )),
            Map.entry("Learn French", List.of(
                    new QuestionTemplate(
                            "The French definite article for masculine singular is…?",
                            List.of("le", "la", "les", "Not sure")
                    )
            )),
            Map.entry("Learn German", List.of(
                    new QuestionTemplate(
                            "The accusative case definite article for ‘der’ changes to…?",
                            List.of("den", "dem", "das", "Not sure")
                    )
            )),
            Map.entry("Learn Japanese", List.of(
                    new QuestionTemplate(
                            "Which script is mainly used for foreign loanwords?",
                            List.of("Katakana", "Hiragana", "Kanji", "Not sure")
                    )
            )),
            Map.entry("Learn Mandarin Chinese", List.of(
                    new QuestionTemplate(
                            "Mandarin has how many basic tones?",
                            List.of("Four", "Two", "Five consonant tones", "Not sure")
                    )
            )),
            Map.entry("Sign Language (ASL)", List.of(
                    new QuestionTemplate(
                            "ASL relies heavily on…?",
                            List.of("Handshape & facial expression", "Spoken English grammar", "Written text", "Not sure")
                    )
            )),
            // Health, Wellness & Lifestyle
            Map.entry("Nutrition & Healthy Eating", List.of(
                    new QuestionTemplate(
                            "Protein’s primary role is to…?",
                            List.of("Build & repair tissues", "Provide quick energy", "Hydrate cells", "Not sure")
                    )
            )),
            Map.entry("Fitness & Exercise Science", List.of(
                    new QuestionTemplate(
                            "VO₂ max measures…?",
                            List.of("Maximum oxygen uptake", "Heart size", "Blood pressure", "Not sure")
                    )
            )),
            Map.entry("Mindfulness & Meditation", List.of(
                    new QuestionTemplate(
                            "Mindfulness meditation focuses on…?",
                            List.of("Present-moment awareness", "Emptying mind of thoughts", "Dream analysis", "Not sure")
                    )
            )),
            Map.entry("Psychology Fundamentals", List.of(
                    new QuestionTemplate(
                            "Classical conditioning was first described by…?",
                            List.of("Ivan Pavlov", "Sigmund Freud", "Carl Rogers", "Not sure")
                    )
            )),
            Map.entry("First Aid & Emergency Response", List.of(
                    new QuestionTemplate(
                            "CPR stands for…?",
                            List.of("Cardiopulmonary resuscitation", "Critical patient recovery", "Care & protect respiration", "Not sure")
                    )
            )),
            Map.entry("Sustainable Living Practices", List.of(
                    new QuestionTemplate(
                            "The 3 R’s stand for…?",
                            List.of("Reduce, Reuse, Recycle", "Research, React, Repair", "Rotate, Replenish, Recover", "Not sure")
                    )
            )),
            Map.entry("Gardening & Horticulture", List.of(
                    new QuestionTemplate(
                            "Photosynthesis occurs mainly in which leaf layer?",
                            List.of("Mesophyll", "Epidermis", "Stomata", "Not sure")
                    )
            )),
            Map.entry("Cooking & Culinary Arts", List.of(
                    new QuestionTemplate(
                            "Sautéing involves cooking food…?",
                            List.of("Quickly in small amount of fat", "Submerged in oil", "In steam only", "Not sure")
                    )
            )),
            Map.entry("Yoga & Flexibility Training", List.of(
                    new QuestionTemplate(
                            "The Sanskrit word ‘asana’ means…?",
                            List.of("Posture", "Breath control", "Meditation", "Not sure")
                    )
            )),
            // Vocational & Practical Skills
            Map.entry("Public Speaking & Presentation Skills", List.of(
                    new QuestionTemplate(
                            "Glossophobia refers to fear of…?",
                            List.of("Public speaking", "Heights", "Closed spaces", "Not sure")
                    )
            )),
            Map.entry("Speed Reading & Comprehension", List.of(
                    new QuestionTemplate(
                            "Sub-vocalisation is…?",
                            List.of("Internal speech when reading", "Skipping lines", "Highlighting text", "Not sure")
                    )
            )),
            Map.entry("Basic Car Maintenance", List.of(
                    new QuestionTemplate(
                            "What does an illuminated oil pressure warning light indicate?",
                            List.of("Low oil pressure", "Full fuel tank", "Battery charge", "Not sure")
                    )
            )),
            Map.entry("Home Repair & DIY Basics", List.of(
                    new QuestionTemplate(
                            "In wiring, the colour green typically denotes…?",
                            List.of("Ground wire", "Live wire", "Neutral wire", "Not sure")
                    )
            )),
            Map.entry("Woodworking Fundamentals", List.of(
                    new QuestionTemplate(
                            "A dovetail joint is prized for…?",
                            List.of("Strong interlocking fit", "Decorative carving", "Metal reinforcement", "Not sure")
                    )
            )),
            Map.entry("Chess Strategy & Tactics", List.of(
                    new QuestionTemplate(
                            "The move 1.e4 is known as…?",
                            List.of("King’s pawn opening", "Queen’s gambit", "Sicilian defence", "Not sure")
                    )
            ))
    );

    private static final Map<String, String> DOMAIN_DESCRIPTIONS = Map.ofEntries(
            // Technology & Computer Science
            Map.entry("Python Programming", "Writing, structuring and debugging Python code from basics to advanced libraries."),
            Map.entry("Web Development - Front-end", "Building user interfaces with HTML, CSS, JavaScript and modern frameworks."),
            Map.entry("Web Development - Back-end", "Server-side logic, APIs and databases using Node.js, Django, Spring, etc."),
            Map.entry("Data Science & Machine Learning", "Collecting, analysing data and training predictive models."),
            Map.entry("Artificial Intelligence Fundamentals", "Core AI concepts, ethics and real-world applications."),
            Map.entry("Cybersecurity Basics", "Understanding threats, vulnerabilities and defence strategies."),
            Map.entry("Cloud Computing (AWS/Azure/GCP)", "On-demand infrastructure, services and DevOps practices in the cloud."),
            Map.entry("Mobile App Development (iOS/Android)", "Designing, coding and publishing native or cross-platform mobile apps."),
            Map.entry("Game Development (Unity/Unreal)", "Creating interactive games using engines, C#/C++ and design principles."),
            Map.entry("Database Management (SQL & NoSQL)", "Designing schemas, querying data and administering database systems."),
            Map.entry("DevOps Practices & Tools", "CI/CD pipelines, containerisation and infrastructure-as-code workflows."),
            Map.entry("Blockchain & Cryptocurrencies", "Distributed ledger principles, smart contracts and crypto-economics."),
            Map.entry("UI/UX Design Principles", "Researching, wireframing and prototyping intuitive user experiences."),
            Map.entry("Quantum Computing Concepts", "Qubits, entanglement and quantum algorithms basics."),
            Map.entry("Internet of Things (IoT) Fundamentals", "Connecting sensors and devices to collect and utilise data."),
            // Business & Finance
            Map.entry("Personal Finance & Investing", "Managing money, budgeting, saving and basic investment vehicles."),
            Map.entry("Digital Marketing & SEO", "Strategies to attract, engage and convert online audiences."),
            Map.entry("Entrepreneurship & Startup Building", "Validating ideas, crafting business models and securing funding."),
            Map.entry("Project Management (Agile & Waterfall)", "Planning, executing and closing projects using Agile/Scrum or traditional methods."),
            Map.entry("Business Analytics", "Turning raw data into actionable business insights."),
            Map.entry("Supply Chain Management", "Coordinating procurement, production, logistics and distribution networks."),
            Map.entry("E-commerce Strategies", "Building and scaling online retail platforms and operations."),
            Map.entry("Stock Market Trading Strategies", "Technical & fundamental analysis, risk management practices."),
            Map.entry("Accounting Fundamentals", "Recording, summarising and reporting financial transactions."),
            Map.entry("Negotiation Skills", "Preparing, communicating and bargaining to reach agreements."),
            // Arts & Humanities
            Map.entry("Creative Writing", "Crafting stories, poems and scripts through plot, character and dialogue."),
            Map.entry("Digital Photography", "Capturing and editing images using composition, lighting and post-processing."),
            Map.entry("Music Theory & Composition", "Understanding scales, chords, harmony and songwriting techniques."),
            Map.entry("Graphic Design (Adobe Suite)", "Visual communication via typography, layout and colour using design software."),
            Map.entry("History (Topic-driven)", "Exploring significant periods, events and cultural developments."),
            Map.entry("Philosophy", "Examining fundamental questions on existence, knowledge and ethics."),
            Map.entry("Learning a Musical Instrument", "Practical techniques, reading notation and performance practice."),
            Map.entry("Drawing & Illustration", "Foundations of sketching, shading and perspective."),
            Map.entry("Film Making Basics", "Storyboarding, cinematography and editing to craft visual stories."),
            Map.entry("Journalism & Media Ethics", "Reporting, fact-checking and ethical standards in news media."),
            // Science & Mathematics
            Map.entry("Physics Fundamentals", "Motion, forces, energy and electromagnetism."),
            Map.entry("Chemistry Basics", "Structure of matter, reactions and bonding principles."),
            Map.entry("Biology", "Study of living organisms, evolution and ecosystems."),
            Map.entry("Mathematics (Calculus, Linear Alg.)", "Core mathematical concepts and real-world applications."),
            Map.entry("Astronomy & Astrophysics", "Celestial bodies, cosmology and the universe’s evolution."),
            Map.entry("Environmental Science", "Earth systems, climate change and sustainability."),
            Map.entry("Neuroscience Basics", "Brain structure and cognitive processes."),
            Map.entry("Genetics & Genomics", "DNA structure, heredity and genetic engineering methods."),
            Map.entry("Geology & Earth Science", "Rocks, plate tectonics and Earth’s history."),
            // Languages
            Map.entry("Learn Spanish", "Grammar, vocabulary and conversational practice in Spanish."),
            Map.entry("Learn French", "Grammar, vocabulary and conversational practice in French."),
            Map.entry("Learn German", "Grammar, cases and speaking proficiency."),
            Map.entry("Learn Japanese", "Hiragana, Katakana, Kanji and conversation."),
            Map.entry("Learn Mandarin Chinese", "Pinyin, tones, characters and conversation."),
            Map.entry("Sign Language (ASL)", "Core signs, grammar and Deaf culture."),
            // Health, Wellness & Lifestyle
            Map.entry("Nutrition & Healthy Eating", "Macro/micronutrients and meal planning for wellbeing."),
            Map.entry("Fitness & Exercise Science", "Physiology, training principles and program design."),
            Map.entry("Mindfulness & Meditation", "Techniques for stress reduction and mental clarity."),
            Map.entry("Psychology Fundamentals", "Cognitive, social and developmental behaviour studies."),
            Map.entry("First Aid & Emergency Response", "Basic life support and handling common injuries."),
            Map.entry("Sustainable Living Practices", "Reducing ecological footprint through daily choices."),
            Map.entry("Gardening & Horticulture", "Plant biology, soil and pest management for healthy gardens."),
            Map.entry("Cooking & Culinary Arts", "Techniques, cuisines and recipe development."),
            Map.entry("Yoga & Flexibility Training", "Asanas, breathing and philosophy for flexibility and balance."),
            // Vocational & Practical Skills
            Map.entry("Public Speaking & Presentation Skills", "Crafting and delivering engaging presentations confidently."),
            Map.entry("Speed Reading & Comprehension", "Techniques to boost reading speed without losing understanding."),
            Map.entry("Basic Car Maintenance", "Routine checks and minor repairs to keep a car running safely."),
            Map.entry("Home Repair & DIY Basics", "Plumbing, electrical and carpentry tasks around the house."),
            Map.entry("Woodworking Fundamentals", "Tool use, joinery and safe workshop practices."),
            Map.entry("Chess Strategy & Tactics", "Openings, middlegame plans and endgame technique.")
    );

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");

        if (userRepository.findByUsername("admin").isEmpty()) {
            UserEntity defaultUser = UserEntity.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin"))
                    .build();
            userRepository.save(defaultUser);
            logger.info("Created default user 'admin'");
        } else {
            logger.info("Default user 'admin' already exists.");
        }

        if (roleRepo.count() == 0) {
            roleRepo.save(RoleEntity.builder().name("ROLE_USER").build());
            roleRepo.save(RoleEntity.builder().name("ROLE_ADMIN").build());
        }

        Map<String, List<String>> catalogue = Map.ofEntries(
                Map.entry("Technology & Computer Science", List.of(
                        "Python Programming",
                        "Web Development - Front-end",
                        "Web Development - Back-end",
                        "Data Science & Machine Learning",
                        "Artificial Intelligence Fundamentals",
                        "Cybersecurity Basics",
                        "Cloud Computing (AWS/Azure/GCP)",
                        "Mobile App Development (iOS/Android)",
                        "Game Development (Unity/Unreal)",
                        "Database Management (SQL & NoSQL)",
                        "DevOps Practices & Tools",
                        "Blockchain & Cryptocurrencies",
                        "UI/UX Design Principles",
                        "Quantum Computing Concepts",
                        "Internet of Things (IoT) Fundamentals"
                )),
                Map.entry("Business & Finance", List.of(
                        "Personal Finance & Investing",
                        "Digital Marketing & SEO",
                        "Entrepreneurship & Startup Building",
                        "Project Management (Agile & Waterfall)",
                        "Business Analytics",
                        "Supply Chain Management",
                        "E-commerce Strategies",
                        "Stock Market Trading Strategies",
                        "Accounting Fundamentals",
                        "Negotiation Skills"
                )),
                Map.entry("Arts & Humanities", List.of(
                        "Creative Writing",
                        "Digital Photography",
                        "Music Theory & Composition",
                        "Graphic Design (Adobe Suite)",
                        "History (Topic-driven)",
                        "Philosophy",
                        "Learning a Musical Instrument",
                        "Drawing & Illustration",
                        "Film Making Basics",
                        "Journalism & Media Ethics"
                )),
                Map.entry("Science & Mathematics", List.of(
                        "Physics Fundamentals",
                        "Chemistry Basics",
                        "Biology",
                        "Mathematics (Calculus, Linear Alg.)",
                        "Astronomy & Astrophysics",
                        "Environmental Science",
                        "Neuroscience Basics",
                        "Genetics & Genomics",
                        "Geology & Earth Science"
                )),
                Map.entry("Languages", List.of(
                        "Learn Spanish",
                        "Learn French",
                        "Learn German",
                        "Learn Japanese",
                        "Learn Mandarin Chinese",
                        "Sign Language (ASL)"
                )),
                Map.entry("Health, Wellness & Lifestyle", List.of(
                        "Nutrition & Healthy Eating",
                        "Fitness & Exercise Science",
                        "Mindfulness & Meditation",
                        "Psychology Fundamentals",
                        "First Aid & Emergency Response",
                        "Sustainable Living Practices",
                        "Gardening & Horticulture",
                        "Cooking & Culinary Arts",
                        "Yoga & Flexibility Training"
                )),
                Map.entry("Vocational & Practical Skills", List.of(
                        "Public Speaking & Presentation Skills",
                        "Speed Reading & Comprehension",
                        "Basic Car Maintenance",
                        "Home Repair & DIY Basics",
                        "Woodworking Fundamentals",
                        "Chess Strategy & Tactics"
                ))
        );

        catalogue.forEach((category, domains) -> domains.forEach(domainName -> {
            domainRepository.findByName(domainName).ifPresentOrElse(
                    existing -> logger.info("'{}' already present", domainName),
                    () -> {
                        DomainEntity domain = DomainEntity.builder()
                                .name(domainName)
                                .description(DOMAIN_DESCRIPTIONS.getOrDefault(domainName, "Explore " + domainName + "."))
                                .category(category)
                                .build();

                        List<AssessmentQuestionEntity> questions = new ArrayList<>();


                        for (QuestionTemplate tpl : GENERIC_QUESTION_TEMPLATES) {
                            questions.add(AssessmentQuestionEntity.builder()
                                    .questionText(tpl.text().replace("[Domain]", domainName))
                                    .options(tpl.options())
                                    .domain(domain)
                                    .build()
                            );
                        }

                        for (QuestionTemplate tpl : CONCEPT_QUESTION_TEMPLATES.getOrDefault(domainName, Collections.emptyList())) {
                            questions.add(AssessmentQuestionEntity.builder()
                                    .questionText(tpl.text())
                                    .options(tpl.options())
                                    .domain(domain)
                                    .build()
                            );
                        }

                        domain.setAssessmentQuestions(questions);
                        domainRepository.save(domain);
                        logger.info("Created domain '{}' in '{}'", domainName, category);
                    }
            );
        }));

        logger.info("Data initialization finished.");
    }
}
