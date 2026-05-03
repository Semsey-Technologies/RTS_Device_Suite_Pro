/**
 * View Backups - Main Application Logic
 */

const app = {
    currentCategory: 'messages',
    currentViewMode: 'list',
    baseUrl: 'https://appassets.androidplatform.net/assets/viewer/',
    dataUrl: 'https://appassets.androidplatform.net/internal/data/',
    mediaUrl: 'https://appassets.androidplatform.net/internal/',

    async init() {
        console.log('App Init: Starting...');
        try {
            // Check if root element exists
            if (!document.getElementById('main-display')) {
                throw new Error('Critical UI elements missing from DOM');
            }

            await this.loadComponents();
            this.setupEventListeners();

            // Short delay to ensure DOM is ready
            setTimeout(() => {
                this.switchCategory('messages');
            }, 100);

            console.log('App Init: Success');
        } catch (error) {
            console.error('App Init: Failed', error);
            document.getElementById('main-display').innerHTML = `
                <div class="error-screen">
                    <h3>Initialization Error</h3>
                    <p>${error.message}</p>
                </div>`;
        }
    },

    async loadComponents() {
        console.log('Loading components...');
        try {
            const topnavResponse = await fetch(`${this.baseUrl}components/topnav.html`);
            if (!topnavResponse.ok) throw new Error('Failed to load topnav');
            const topnavHtml = await topnavResponse.text();
            document.getElementById('topnav-container').innerHTML = topnavHtml;
        } catch (e) {
            console.error('Error loading components:', e);
            throw e;
        }
    },

    setupEventListeners() {
        document.addEventListener('click', (e) => {
            const tab = e.target.closest('.nav-tab');
            if (tab) {
                const category = tab.dataset.category;
                this.switchCategory(category);
            }

            const convItem = e.target.closest('.conversation-item');
            if (convItem) {
                const threadId = convItem.dataset.threadId;
                this.loadConversation(threadId);
            }
        });
    },

    async switchCategory(category) {
        console.log('Switching to category:', category);
        this.currentCategory = category;

        document.querySelectorAll('.nav-tab').forEach(t => {
            t.classList.toggle('active', t.dataset.category === category);
        });

        const titleElem = document.getElementById('category-title');
        if (titleElem) titleElem.textContent = category.charAt(0).toUpperCase() + category.slice(1);

        // Clear display before loading
        document.getElementById('main-display').innerHTML = '<div class="loading">Loading data...</div>';
        document.getElementById('sidebar-container').innerHTML = '';

        try {
            switch(category) {
                case 'messages':
                    await this.renderMessagesView();
                    break;
                case 'calls':
                    await this.renderCallsView();
                    break;
                case 'contacts':
                    await this.renderContactsView();
                    break;
                case 'files':
                    await this.renderFilesView();
                    break;
                case 'pictures':
                case 'videos':
                    await this.renderGalleryView(category);
                    break;
                default:
                    document.getElementById('main-display').innerHTML = `<div class="info">View for ${category} coming soon.</div>`;
            }
        } catch (e) {
            console.error('Error rendering category:', e);
            document.getElementById('main-display').innerHTML = `
                <div class="empty-state">
                    <h3>No Data Available</h3>
                    <p>The ${category} data was not found in this backup or failed to load.</p>
                    <small style="color: #666;">Error: ${e.message}</small>
                </div>`;
        }
    },

    async renderMessagesView() {
        console.log('Fetching conversations from:', `${this.dataUrl}conversations.json`);
        const response = await fetch(`${this.dataUrl}conversations.json`);
        if (!response.ok) {
            console.error('Conversations fetch failed:', response.status, response.statusText);
            throw new Error(`conversations.json not found (${response.status})`);
        }
        const conversations = await response.json();
        console.log('Conversations loaded:', conversations);

        if (conversations.length === 0) {
            document.getElementById('main-display').innerHTML = '<div class="empty-state">No conversations found</div>';
            return;
        }

        let sidebarHtml = '<div class="sidebar-header">Conversations</div>';
        conversations.forEach(c => {
            sidebarHtml += `
                <div class="conversation-item" data-thread-id="${c.thread_id}">
                    <div class="conv-info">
                        <strong>${c.contact}</strong>
                        <p>${c.last_message}</p>
                    </div>
                    <span class="conv-time">${this.formatDate(c.timestamp)}</span>
                </div>
            `;
        });
        document.getElementById('sidebar-container').innerHTML = sidebarHtml;
        document.getElementById('main-display').innerHTML = '<div class="empty-state">Select a conversation from the sidebar</div>';
    },

    async loadConversation(threadId) {
        try {
            const response = await fetch(`${this.dataUrl}messages_${threadId}.json`);
            if (!response.ok) throw new Error('Message data not found');
            const messages = await response.json();

            let chatHtml = '<div class="chat-container">';
            messages.forEach(m => {
                const mmsHtml = m.mms ? this.renderMMS(m.mms) : '';
                chatHtml += `
                    <div class="message-bubble ${m.type}">
                        ${mmsHtml}
                        <div class="message-text">${m.body}</div>
                        <span class="message-timestamp">${this.formatDate(m.timestamp)}</span>
                    </div>
                `;
            });
            chatHtml += '</div>';
            document.getElementById('main-display').innerHTML = chatHtml;
        } catch (e) {
            document.getElementById('main-display').innerHTML = `<div class="error">Error: ${e.message}</div>`;
        }
    },

    renderMMS(mms) {
        const fullPath = this.mediaUrl + mms.path;
        if (mms.type === 'image') {
            return `<img src="${fullPath}" class="mms-media" style="max-width: 100%; border-radius: 8px;">`;
        } else if (mms.type === 'video') {
            return `<video controls class="mms-media" style="max-width: 100%;"><source src="${fullPath}" type="video/mp4"></video>`;
        }
        return '';
    },

    async renderCallsView() {
        document.getElementById('sidebar-container').innerHTML = '';
        try {
            const response = await fetch(`${this.dataUrl}calls.json`);
            if (!response.ok) throw new Error('Call logs not found');
            const calls = await response.json();

            let html = '<div class="data-table-container"><table>';
            html += '<thead><tr><th>Type</th><th>Number/Name</th><th>Date</th><th>Duration</th></tr></thead><tbody>';
            calls.forEach(c => {
                html += `
                    <tr>
                        <td><span class="call-type-badge ${c.type}">${c.type}</span></td>
                        <td><strong>${c.name}</strong><br><small>${c.number}</small></td>
                        <td>${this.formatFullDate(c.date)}</td>
                        <td>${this.formatDuration(c.duration)}</td>
                    </tr>
                `;
            });
            html += '</tbody></table></div>';
            document.getElementById('main-display').innerHTML = html;
        } catch (e) {
            document.getElementById('main-display').innerHTML = `<div class="error">${e.message}</div>`;
        }
    },

    async renderContactsView() {
        document.getElementById('sidebar-container').innerHTML = '';
        try {
            const response = await fetch(`${this.dataUrl}contacts.json`);
            if (!response.ok) throw new Error('Contacts not found');
            const contacts = await response.json();

            let html = '<div class="contacts-grid">';
            contacts.forEach(c => {
                html += `
                    <div class="contact-card">
                        <div class="contact-avatar">${c.name.charAt(0)}</div>
                        <div class="contact-details">
                            <strong>${c.name}</strong>
                            <p>${c.phone || ''}</p>
                            <small>${c.email || ''}</small>
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            document.getElementById('main-display').innerHTML = html;
        } catch (e) {
            document.getElementById('main-display').innerHTML = `<div class="error">${e.message}</div>`;
        }
    },

    async renderFilesView() {
        document.getElementById('sidebar-container').innerHTML = '';
        try {
            const response = await fetch(`${this.dataUrl}files.json`);
            if (!response.ok) throw new Error('Files not found');
            const files = await response.json();

            let html = '<div class="files-list">';
            files.forEach(f => {
                const icon = this.getFileIcon(f.mimeType);
                html += `
                    <div class="file-item">
                        <span class="file-icon">${icon}</span>
                        <div class="file-info">
                            <strong>${f.name}</strong>
                            <span>${(f.size / 1024).toFixed(1)} KB</span>
                        </div>
                        <a href="${this.mediaUrl}${f.path}" target="_blank" class="file-action">View</a>
                    </div>
                `;
            });
            html += '</div>';
            document.getElementById('main-display').innerHTML = html;
        } catch (e) {
            document.getElementById('main-display').innerHTML = `<div class="error">${e.message}</div>`;
        }
    },

    getFileIcon(mime) {
        if (mime.startsWith('image/')) return '🖼️';
        if (mime.startsWith('video/')) return '🎥';
        if (mime.startsWith('audio/')) return '🎵';
        if (mime.includes('pdf')) return '📕';
        return '📄';
    },

    formatDate(timestamp) {
        return new Date(timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    },

    formatFullDate(timestamp) {
        return new Date(timestamp).toLocaleString();
    },

    formatDuration(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}m ${secs}s`;
    }
};

window.app = app;
document.addEventListener('DOMContentLoaded', () => app.init());