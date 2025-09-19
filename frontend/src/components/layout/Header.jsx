import React, { useState, useEffect } from 'react';
import { AppBar, Toolbar, IconButton, Typography, Avatar, Box, Stack } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import LogoutIcon from '@mui/icons-material/Logout';
import { useNavigate, useLocation } from 'react-router-dom';
import {getProfile} from '../../services/api.js';
import {useLearningContext} from "../../contexts/LearningContext.jsx";

export default function Header() {
    const [profile, setProfile] = useState(null);
    const nav = useNavigate();
    const loc = useLocation();
    const { state, logout } = useLearningContext();
    const { profileVersion } = state;

    useEffect(() => {
        let mounted = true;
        getProfile()
            .then(res => mounted && setProfile(res.data))
            .catch(console.error);
        return () => { mounted = false; };
    }, [profileVersion]);

    const handleLogout = async () => {
        logout();
        nav('/login');
    };

    return (
        <AppBar position="static" color="primary">
            <Toolbar>
                {loc.pathname !== '/' && (
                    <IconButton edge="start" color="inherit" onClick={() => nav('/') }>
                        <HomeIcon />
                    </IconButton>
                )}

                <Typography variant="h6" sx={{ flexGrow: 1 }}>
                    Adaptive Learning
                </Typography>

                {profile && (
                    <Box display="flex" alignItems="center" gap={1}>
                        <Avatar>{profile.username[0].toUpperCase()}</Avatar>
                        <Stack spacing={0.5}>
                            <Typography variant="subtitle2">{profile.username}</Typography>
                            <Typography variant="caption">
                                Started domains: {profile.startedDomains ?? 0}
                            </Typography>
                            <Typography variant="caption">
                                Completed insights: {profile.completedInsights ?? 0}
                            </Typography>
                        </Stack>
                        <IconButton color="inherit" onClick={handleLogout} title="Log out">
                            <LogoutIcon />
                        </IconButton>
                    </Box>
                )}
            </Toolbar>
        </AppBar>
    );
}