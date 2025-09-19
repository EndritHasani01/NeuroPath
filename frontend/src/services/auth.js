import apiClient from "./api";
export const loginUser   = (creds) => apiClient.post("/auth/login", creds);
export const registerUser= (creds) => apiClient.post("/auth/register", creds);
