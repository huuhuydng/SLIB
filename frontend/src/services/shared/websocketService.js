import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { API_BASE_URL } from '../../config/apiConfig';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map(); // Map<topic, subscription>
    this.callbacks = new Map(); // Map<topic, Set<callback>>
    this.onConnectedCallbacks = new Set();
    this.onErrorCallbacks = new Set();
  }

  getAuthHeaders() {
    const token =
      localStorage.getItem('kiosk_device_token') ||
      sessionStorage.getItem('librarian_token') ||
      localStorage.getItem('librarian_token') ||
      sessionStorage.getItem('kiosk_device_token');

    return token ? { Authorization: `Bearer ${token}` } : null;
  }

  connect(onConnected, onError) {
    if (onConnected) {
      this.onConnectedCallbacks.add(onConnected);
    }
    if (onError) {
      this.onErrorCallbacks.add(onError);
    }

    if (this.client?.connected) {
      this.connected = true;
      if (onConnected) onConnected();
      return;
    }

    if (this.client?.active && !this.client.connected) {
      return;
    }

    const connectHeaders = this.getAuthHeaders();
    if (!connectHeaders) {
      if (onError) onError(new Error('Thiếu token để kết nối WebSocket'));
      return;
    }

    try {
      // Create SockJS connection
      const socket = new SockJS(`${API_BASE_URL}/ws`);

      // Create STOMP client
      this.client = new Client({
        webSocketFactory: () => socket,
        connectHeaders,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      // Set up connection handlers
      this.client.onConnect = () => {
        this.connected = true;
        this.resubscribeAll();
        this.onConnectedCallbacks.forEach((callback) => {
          try {
            callback();
          } catch (error) {
            console.error('WebSocket onConnected callback error:', error);
          }
        });
      };

      this.client.onStompError = (frame) => {
        this.connected = false;
        this.onErrorCallbacks.forEach((callback) => {
          try {
            callback(frame);
          } catch (error) {
            console.error('WebSocket onError callback error:', error);
          }
        });
      };

      this.client.onWebSocketClose = () => {
        this.connected = false;
        this.subscriptions.clear();
      };

      // Activate the client
      this.client.activate();
    } catch (error) {
      this.connected = false;
      if (onError) onError(error);
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
      this.subscriptions.clear();
      this.callbacks.clear();
      this.client = null;
    }
  }

  subscribe(topic, callback) {
    if (!this.client?.connected) {
      return null;
    }

    // Initialize callbacks set for this topic if not exists
    if (!this.callbacks.has(topic)) {
      this.callbacks.set(topic, new Set());
    }

    // Add callback to the set
    this.callbacks.get(topic).add(callback);

    // Only subscribe to STOMP topic once
    if (!this.subscriptions.has(topic)) {
      if (!this.ensureTopicSubscription(topic)) {
        return null;
      }
    }

    // Return unsubscribe function for this specific callback
    return () => {
      this.unsubscribeCallback(topic, callback);
    };
  }

  ensureTopicSubscription(topic) {
    if (!this.client?.connected || this.subscriptions.has(topic)) {
      return !!this.subscriptions.get(topic);
    }

    try {
      const subscription = this.client.subscribe(topic, (message) => {
        try {
          const data = JSON.parse(message.body);

          // Notify all registered callbacks for this topic
          const topicCallbacks = this.callbacks.get(topic);
          if (topicCallbacks) {
            topicCallbacks.forEach(cb => {
              try {
                cb(data);
              } catch (error) {
                console.error('WebSocket callback error:', error);
              }
            });
          }
        } catch (error) {
          console.error('WebSocket parse error:', error);
        }
      });

      this.subscriptions.set(topic, subscription);
      return true;
    } catch (error) {
      console.error('WebSocket subscribe error:', error);
      return false;
    }
  }

  resubscribeAll() {
    for (const topic of this.callbacks.keys()) {
      this.ensureTopicSubscription(topic);
    }
  }

  unsubscribeCallback(topic, callback) {
    const topicCallbacks = this.callbacks.get(topic);
    if (topicCallbacks) {
      topicCallbacks.delete(callback);

      // If no more callbacks for this topic, unsubscribe from STOMP
      if (topicCallbacks.size === 0) {
        this.callbacks.delete(topic);
        const subscription = this.subscriptions.get(topic);
        if (subscription) {
          subscription.unsubscribe();
          this.subscriptions.delete(topic);
        }
      }
    }
  }

  unsubscribe(topic) {
    // Unsubscribe all callbacks for this topic
    this.callbacks.delete(topic);
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  isConnected() {
    return !!this.client?.connected;
  }
}

// Export a singleton instance
export default new WebSocketService();
