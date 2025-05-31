import { useState } from "react";
import {
    TextField,
    Button,
    Card,
    CardContent,
    Typography,
    Link,
} from "@mui/material";
import { registerUser } from "../services/auth";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

export default function RegisterPage() {
    const [form, setForm] = useState({
        username: "",
        password: "",
        confirmPassword: "",
    });
    const [error, setError] = useState("");
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleChange = (e) =>
        setForm({ ...form, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (form.password !== form.confirmPassword) {
            setError("Passwords do not match");
            return;
        }
        try {
            await registerUser(form);
            const { data } = await import("../services/auth").then((m) =>
                m.loginUser({ username: form.username, password: form.password })
            );
            login(data.token);
            navigate("/");
        } catch (err) {
            setError(err.response?.data ?? "Registration failed");
        }
    };

    return (
        <Card sx={{ maxWidth: 400, mx: "auto", mt: 6 }}>
            <CardContent component="form" onSubmit={handleSubmit}>
                <Typography variant="h5" gutterBottom>
                    Sign up
                </Typography>

                <TextField
                    margin="dense"
                    label="E-mail or username"
                    name="username"
                    fullWidth
                    value={form.username}
                    onChange={handleChange}
                    required
                />
                <TextField
                    margin="dense"
                    label="Password"
                    type="password"
                    name="password"
                    fullWidth
                    value={form.password}
                    onChange={handleChange}
                    required
                />
                <TextField
                    margin="dense"
                    label="Confirm password"
                    type="password"
                    name="confirmPassword"
                    fullWidth
                    value={form.confirmPassword}
                    onChange={handleChange}
                    required
                />

                {error && (
                    <Typography color="error" variant="body2">
                        {error}
                    </Typography>
                )}

                <Button sx={{ mt: 2 }} type="submit" variant="contained" fullWidth>
                    Create account
                </Button>

                <Typography sx={{ mt: 1 }} variant="body2" align="center">
                    Already have one?{" "}
                    <Link underline="hover" onClick={() => navigate("/login")}>
                        Sign in
                    </Link>
                </Typography>
            </CardContent>
        </Card>
    );
}
