import React, { useEffect, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLearningContext } from '../contexts/LearningContext.jsx';
import {
    Box, Typography, List, ListItem, ListItemButton,
    ListItemText, Paper, Divider
} from '@mui/material';


const groupBy = (arr, keyFn) => arr.reduce((acc, item) => {
    const k = keyFn(item);
    (acc[k] = acc[k] || []).push(item);
    return acc;
}, {});

function DomainSelectorPage() {
    const { state, fetchDomains, selectDomain } = useLearningContext();
    const navigate = useNavigate();

    useEffect(() => { fetchDomains(); }, [fetchDomains]);

    const handleDomainSelect = (domain) => {
        selectDomain(domain);
        navigate(domain.inProgress ? `/domain/${domain.id}` : `/assessment`);
    };

    const inProgress   = state.domains.filter(d => d.inProgress);
    const notStarted   = state.domains.filter(d => !d.inProgress);
    const grouped      = groupBy(notStarted, d => d.category);

    if (state.isLoading && state.domains.length === 0)
        return <Typography>Loading domains…</Typography>;

    return (
        <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom component="h2">
                Choose a Domain to Study
            </Typography>

            {inProgress.length > 0 && (
                <Box sx={{ mb: 4 }}>
                    <Typography variant="subtitle1" sx={{ mb: 1 }}>
                        Continue learning
                    </Typography>
                    <List>
                        {inProgress.map(d => (
                            <Fragment key={d.id}>
                                <ListItem disablePadding>
                                    <ListItemButton onClick={() => handleDomainSelect(d)}>
                                        <ListItemText primary={d.name} secondary={d.description} />
                                    </ListItemButton>
                                </ListItem>
                                <Divider component="li" />
                            </Fragment>
                        ))}
                    </List>
                </Box>
            )}

            <Typography variant="subtitle1" sx={{ mb: 1 }}>
                Start a new domain
            </Typography>

            {Object.entries(grouped).map(([cat, domains]) => (
                <Box key={cat} sx={{ mb: 3 }}>
                    <Typography variant="body1" sx={{ fontWeight: 600, mt: 2, mb: 1 }}>
                        {cat}
                    </Typography>
                    <List dense>
                        {domains.map(d => (
                            <Fragment key={d.id}>
                                <ListItem disablePadding>
                                    <ListItemButton onClick={() => handleDomainSelect(d)}>
                                        <ListItemText primary={d.name} secondary={d.description} />
                                    </ListItemButton>
                                </ListItem>
                                <Divider component="li" />
                            </Fragment>
                        ))}
                    </List>
                </Box>
            ))}
        </Paper>
    );
}

export default DomainSelectorPage;
