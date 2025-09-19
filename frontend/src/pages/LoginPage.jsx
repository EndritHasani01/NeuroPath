import { useState } from "react";
import { TextField, Button, Card, CardContent, Typography, Link } from "@mui/material";
import { loginUser } from "../services/auth";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

export default function LoginPage() {
    const [form, setForm] = useState({ username: "", password: "" });
    const [error, setError] = useState("");
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleChange = (e) =>
        setForm({ ...form, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const { data } = await loginUser(form);
            login(data.token);
            navigate("/");
        } catch (err) {
            setError("Incorrect credentials");
        }
    };

    return (
        <Card sx={{ maxWidth: 400, mx: "auto", mt: 6 }}>
            <CardContent component="form" onSubmit={handleSubmit}>
                <Typography variant="h5" gutterBottom>
                    Sign in
                </Typography>
                <TextField
                    margin="dense"
                    label="Username or email"
                    name="username"
                    fullWidth
                    value={form.username}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    label="Password"
                    type="password"
                    name="password"
                    fullWidth
                    value={form.password}
                    onChange={handleChange}
                />
                {error && (
                    <Typography color="error" variant="body2">
                        {error}
                    </Typography>
                )}
                <Button sx={{ mt: 2 }} type="submit" variant="contained" fullWidth>
                    Login
                </Button>

                <Typography sx={{ mt: 1 }} variant="body2" align="center">
                   First time here?{" "}
                   <Link underline="hover" onClick={() => navigate("/register")}>
                     Create an account
                   </Link>
                </Typography>
            </CardContent>
        </Card>
    );
}
