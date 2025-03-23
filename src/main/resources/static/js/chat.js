document.addEventListener('DOMContentLoaded', function() {
    // Check authentication
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    if (!token) {
        window.location.href = '/auth';
        return;
    }

    // DOM elements
    const userList = document.getElementById('user-list');
    const messageList = document.getElementById('message-list');
    const messageForm = document.getElementById('message-form');
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    const chatTitle = document.getElementById('chat-title');
    const usernameSpan = document.getElementById('username');
    const logoutBtn = document.getElementById('logout-btn');
    const showUsersBtn = document.getElementById('show-users-btn');
    const userListContainer = document.getElementById('user-list-container');
    const backBtn = document.getElementById('back-btn');
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toast-message');

    // Set username in header
    usernameSpan.textContent = user.username;

    // Variables
    let stompClient = null;
    let selectedUserId = null;

    // Connect to WebSocket
    function connectWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        // Disable debug logs
        stompClient.debug = null;

        stompClient.connect(
            {},
            onConnected,
            onError
        );
    }

    function onConnected() {
        // Subscribe to the user's private channel
        stompClient.subscribe(`/user/queue/messages`, onMessageReceived);

        // Subscribe to the public channel for user list updates
        stompClient.subscribe('/topic/users', onUserListUpdated);

        // Subscribe to error channel
        stompClient.subscribe('/user/queue/errors', onErrorReceived);

        // Send a connection message with JWT token
        stompClient.send(
            '/app/chat',
            {'content-type': 'application/json'},
            JSON.stringify({
                type: 'connection',
                payload: {},
                token: token
            })
        );
    }

    function onError(error) {
        console.error('WebSocket Error:', error);
        showToast('Failed to connect to the chat server. Please try again.');

        // Try to reconnect after 5 seconds
        setTimeout(connectWebSocket, 5000);
    }

    function onMessageReceived(payload) {
        const message = JSON.parse(payload.body);

        switch (message.type) {
            case 'message':
                if (selectedUserId) {
                    addMessageToChat(message.payload.message);
                }
                break;

            case 'history':
                displayMessageHistory(message.payload.messages);
                break;
        }
    }

    function onUserListUpdated(payload) {
        const data = JSON.parse(payload.body);
        if (data.type === 'users') {
            updateUserList(data.payload.users);
        }
    }

    function onErrorReceived(payload) {
        const errorData = JSON.parse(payload.body);
        showToast(errorData.payload.message);
    }

    function updateUserList(users) {
        userList.innerHTML = '';

        users.forEach(user => {
            if (user.id !== window.user.id) {
                const li = document.createElement('li');
                li.className = 'user-item p-4 hover:bg-gray-50 cursor-pointer flex items-center justify-between';
                li.dataset.userId = user.id;

                const userInfo = document.createElement('div');
                userInfo.className = 'flex items-center';

                const avatar = document.createElement('div');
                avatar.className = 'w-10 h-10 rounded-full bg-blue-500 flex items-center justify-center text-white font-semibold mr-3';
                avatar.textContent = user.username.charAt(0).toUpperCase();

                const name = document.createElement('span');
                name.textContent = user.username;

                userInfo.appendChild(avatar);
                userInfo.appendChild(name);

                const status = document.createElement('span');
                status.className = `w-3 h-3 rounded-full ${user.isOnline ? 'bg-green-500' : 'bg-gray-400'}`;

                li.appendChild(userInfo);
                li.appendChild(status);

                li.addEventListener('click', () => selectUser(user));

                userList.appendChild(li);
            }
        });
    }

    function selectUser(user) {
        selectedUserId = user.id;
        chatTitle.textContent = user.username;

        // Enable message input
        messageInput.disabled = false;
        sendBtn.disabled = false;

        // Clear message list
        messageList.innerHTML = '';

        // Request message history
        stompClient.send(
            '/app/chat',
            {'content-type': 'application/json'},
            JSON.stringify({
                type: 'requestHistory',
                payload: {
                    targetUserId: selectedUserId
                },
                token: token
            })
        );

        // On mobile, hide user list
        if (window.innerWidth < 768) {
            userListContainer.classList.add('hidden');
        }
    }

    function displayMessageHistory(messages) {
        messageList.innerHTML = '';

        if (messages.length === 0) {
            const emptyState = document.createElement('div');
            emptyState.className = 'text-center text-gray-500 my-8';
            emptyState.textContent = 'No messages yet. Start the conversation!';
            messageList.appendChild(emptyState);
        } else {
            messages.forEach(message => {
                addMessageToChat(message);
            });

            // Scroll to bottom
            messageList.scrollTop = messageList.scrollHeight;
        }
    }

    function addMessageToChat(message) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `mb-4 ${message.isCurrentUser ? 'ml-auto max-w-3/4 text-right' : 'mr-auto max-w-3/4'}`;

        const bubble = document.createElement('div');
        bubble.className = `inline-block p-3 rounded-lg ${message.isCurrentUser ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-800'}`;

        const text = document.createElement('p');
        text.textContent = message.text;

        const time = document.createElement('p');
        time.className = 'text-xs mt-1 text-gray-500';
        time.textContent = formatMessageTime(new Date(message.timestamp));

        bubble.appendChild(text);
        messageDiv.appendChild(bubble);
        messageDiv.appendChild(time);

        messageList.appendChild(messageDiv);

        // Scroll to bottom
        messageList.scrollTop = messageList.scrollHeight;
    }

    function formatMessageTime(date) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function sendMessage(text) {
        if (!text.trim() || !selectedUserId) return;

        stompClient.send(
            '/app/chat',
            {'content-type': 'application/json'},
            JSON.stringify({
                type: 'message',
                payload: {
                    text: text.trim(),
                    targetUserId: selectedUserId
                },
                token: token
            })
        );

        // Clear input
        messageInput.value = '';
    }

    function showToast(message) {
        toastMessage.textContent = message;

        // Show toast
        toast.classList.remove('translate-y-20', 'opacity-0');

        // Hide toast after 3 seconds
        setTimeout(() => {
            toast.classList.add('translate-y-20', 'opacity-0');
        }, 3000);
    }

    // Event listeners
    messageForm.addEventListener('submit', function(e) {
        e.preventDefault();
        sendMessage(messageInput.value);
    });

    logoutBtn.addEventListener('click', function() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/auth';
    });

    showUsersBtn.addEventListener('click', function() {
        userListContainer.classList.remove('hidden');
    });

    backBtn.addEventListener('click', function() {
        userListContainer.classList.add('hidden');
    });

    // Initialize
    connectWebSocket();
});