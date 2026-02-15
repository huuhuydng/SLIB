# Frontend Development Reference

Chi tiết về phát triển frontend React cho SLIB.

## Tech Stack

- React 19 + Vite 7.3
- TailwindCSS 4.x
- React Router DOM 7.11
- Axios, STOMP.js, TipTap

## Component Pattern

```jsx
import { useState, useEffect } from 'react';
import { resourceService } from '../services/resourceService';
import './ResourceList.css';

export default function ResourceList() {
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetchResources();
  }, []);
  
  const fetchResources = async () => {
    try {
      setLoading(true);
      const data = await resourceService.getAll();
      setResources(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) return <div className="loading">Dang tai...</div>;
  if (error) return <div className="error">{error}</div>;
  
  return (
    <div className="resource-list">
      {resources.map(resource => (
        <ResourceCard key={resource.id} resource={resource} />
      ))}
    </div>
  );
}
```

## Service Pattern

```javascript
// services/resourceService.js
import api from './api';

export const resourceService = {
  getAll: async () => {
    const response = await api.get('/slib/resources');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await api.get(`/slib/resources/${id}`);
    return response.data;
  },
  
  create: async (data) => {
    const response = await api.post('/slib/resources', data);
    return response.data;
  },
  
  update: async (id, data) => {
    const response = await api.put(`/slib/resources/${id}`, data);
    return response.data;
  },
  
  delete: async (id) => {
    await api.delete(`/slib/resources/${id}`);
  }
};
```

## API Configuration

```javascript
// services/api.js
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
```

## CSS Naming

Use BEM-inspired kebab-case:

```css
/* ResourceList.css */
.resource-list {
  display: grid;
  gap: 1rem;
}

.resource-card {
  padding: 1rem;
  border-radius: 8px;
}

.resource-card--active {
  border: 2px solid var(--primary);
}

.resource-card__title {
  font-weight: 600;
}

.resource-card__status {
  font-size: 0.875rem;
}
```

## Route Structure

```jsx
// routes/AdminRoutes.jsx
import { Routes, Route } from 'react-router-dom';
import AdminLayout from '../layouts/AdminLayout';

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="areas" element={<AreasPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="config" element={<ConfigPage />} />
      </Route>
    </Routes>
  );
}
```

## WebSocket Chat

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const connectWebSocket = (conversationId, onMessage) => {
  const socket = new SockJS(`${API_URL}/ws`);
  const client = Stomp.over(socket);
  
  client.connect({}, () => {
    client.subscribe(`/topic/chat/${conversationId}`, (message) => {
      onMessage(JSON.parse(message.body));
    });
  });
  
  return client;
};
```
