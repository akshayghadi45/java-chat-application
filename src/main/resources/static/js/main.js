document.addEventListener('DOMContentLoaded', function() {
    // Check if user is already logged in
    const token = localStorage.getItem('token');
    if (token) {
        // Add "Continue to Chat" button
        const actionDiv = document.querySelector('.flex.flex-col.sm\\:flex-row');

        const continueLink = document.createElement('a');
        continueLink.href = '/chat';
        continueLink.className = 'inline-block bg-green-600 hover:bg-green-700 text-white font-semibold py-3 px-6 rounded-lg text-center transition duration-200';
        continueLink.textContent = 'Continue to Chat';

        actionDiv.insertBefore(continueLink, actionDiv.firstChild);
    }
});