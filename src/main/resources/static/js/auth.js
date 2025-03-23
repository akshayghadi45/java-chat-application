document.addEventListener('DOMContentLoaded', function() {
    // Check if user is already logged in
    const token = localStorage.getItem('token');
    if (token) {
        // Verify the token is valid
        fetch('/api/profile', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => {
                if (response.ok) {
                    window.location.href = '/chat';
                }
            })
            .catch(error => {
                console.error('Error verifying token:', error);
                localStorage.removeItem('token');
            });
    }

    // Tab switching
    const loginTab = document.getElementById('login-tab');
    const registerTab = document.getElementById('register-tab');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    loginTab.addEventListener('click', function() {
        loginTab.classList.add('active-tab');
        registerTab.classList.remove('active-tab');
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
    });
    registerTab.addEventListener('click', function() {
        registerTab.classList.add('active-tab');
        loginTab.classList.remove('active-tab');
        registerForm.classList.remove('hidden');
        loginForm.classList.add('hidden');
    });
    // Login form submission
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;
        const errorElement = document.getElementById('login-error');

        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username,
                password
            })
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('user', JSON.stringify(data.user));
                    window.location.href = '/chat';
                } else {
                    errorElement.textContent = data.message;
                    errorElement.classList.remove('hidden');
                }
            })
            .catch(error => {
                console.error('Error logging in:', error);
                errorElement.textContent = 'An error occurred. Please try again.';
                errorElement.classList.remove('hidden');
            });
    });
    // Register form submission
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const username = document.getElementById('register-username').value;
        const password = document.getElementById('register-password').value;
        const confirmPassword = document.getElementById('register-confirm-password').value;
        const errorElement = document.getElementById('register-error');

        if (password !== confirmPassword) {
            errorElement.textContent = 'Passwords do not match.';
            errorElement.classList.remove('hidden');
            return;
        }

        fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username,
                password
            })
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('user', JSON.stringify(data.user));
                    window.location.href = '/chat';
                } else {
                    errorElement.textContent = data.message;
                    errorElement.classList.remove('hidden');
                }
            })
            .catch(error => {
                console.error('Error registering:', error);
                errorElement.textContent = 'An error occurred. Please try again.';
                errorElement.classList.remove('hidden');
            });
    });
});