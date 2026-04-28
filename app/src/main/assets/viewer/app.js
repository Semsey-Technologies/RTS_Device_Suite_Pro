/**
 * View Backups - Native Extension JavaScript
 */

const state = {
    currentCategory: 'messages',
    viewMode: 'grid',
    data: {
        pictures: [],
        videos: [],
        documents: [],
        messages: [],
        contacts: [],
        audio: [],
        other: []
    },
    selectedThreadId: null,
    selectedItem: null,
    searchQuery: '',
    messagePage: 1,
    pageSize: 20
};

// Initialize the app
document.addEventListener('DOMContentLoaded', () => {
    console.log("Viewer DOMContentLoaded");
    setupEventListeners();

    // Fix: Show something immediately if data is slow or empty
    renderSidebar();
    renderMainContent();

    // Request data from Android
    if (window.Android) {
        console.log("Requesting data from Android...");
        window.Android.requestData();
    } else {
        console.log("Android bridge not found, loading mock data");
        loadMockData();
    }

    // Safety timeout to hide overlay if something hangs
    setTimeout(hideOverlay, 5000);
});

function setupEventListeners() {
    // Top Nav
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', () => {
            const cat = item.getAttribute('data-category');
            switchCategory(cat);
        });
    });

    // Search
    document.getElementById('search-input').addEventListener('input', (e) => {
        state.searchQuery = e.target.value.toLowerCase();
        renderSidebar();
    });

    // View Mode
    document.getElementById('view-mode-selector').addEventListener('change', (e) => {
        state.viewMode = e.target.value;
        renderMainContent();
    });
}

function initDataFetch() {
    console.log("initDataFetch called, fetching from https://app.local/data.json");
    fetch('https://app.local/data.json')
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok: ' + response.status);
            return response.json();
        })
        .then(data => {
            console.log("Data fetched via interceptor successfully");
            loadData(data);
        })
        .catch(error => {
            console.error("Error fetching data:", error);
            const display = document.getElementById('viewer-display');
            if (display) {
                display.innerHTML = `<div class="empty-state">Error loading backup data: ${error.message}</div>`;
            }
            hideOverlay();
        });
}

function loadDataBase64(base64Str) {
    // Deprecated to avoid OOM, but keeping for compatibility if needed
    console.log("loadDataBase64 called (deprecated)");
    try {
        const json = atob(base64Str);
        const data = JSON.parse(json);
        loadData(data);
    } catch (e) {
        console.error("Error in loadDataBase64:", e);
    }
}

function hideOverlay() {
    console.log("hideOverlay called - Forcefully removing overlay");
    const overlay = document.getElementById('loading-overlay');
    if (overlay) {
        overlay.remove();
    }
}

/**
 * Called by Android via evaluateJavascript
 */
function loadData(inputData) {
    console.log("loadData received data, type:", typeof inputData);

    // Force overlay hide immediately so user sees the structure
    hideOverlay();

    try {
        if (typeof inputData === 'string') {
            state.data = JSON.parse(inputData);
        } else {
            state.data = inputData;
        }

        const counts = Object.keys(state.data).map(k => `${k}: ${Array.isArray(state.data[k]) ? state.data[k].length : '0'}`);
        console.log("Data loaded successfully. Counts: " + counts.join(', '));

        // Fix: Ensure we are in a valid category
        if (!state.currentCategory) state.currentCategory = 'pictures';

        renderSidebar();
        renderMainContent();

        // Hide overlay AFTER rendering attempt
        hideOverlay();
    } catch (e) {
        console.error("Error in loadData:", e);
        const display = document.getElementById('viewer-display');
        if (display) display.innerHTML = `<div class="empty-state">Error parsing data: ${e.message}</div>`;
        hideOverlay();
    }
}

function switchCategory(category) {
    state.currentCategory = category;
    state.selectedItem = null;
    state.selectedThreadId = null;

    // Update UI
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.getAttribute('data-category') === category);
    });

    const titleMap = {
        'messages': 'Text Messages',
        'contacts': 'Contacts',
        'pictures': 'Pictures',
        'videos': 'Videos',
        'audio': 'Music',
        'documents': 'Documents'
    };
    document.getElementById('current-title').textContent = titleMap[category] || (category.charAt(0).toUpperCase() + category.slice(1));

    // Show/Hide View Mode Selector (relevant for media)
    const mediaCats = ['pictures', 'videos', 'audio', 'documents', 'other'];
    document.getElementById('view-mode-container').style.display = mediaCats.includes(category) ? 'block' : 'none';

    renderSidebar();
    renderMainContent();
}

function renderSidebar() {
    const container = document.getElementById('sidebar-content');
    container.innerHTML = '';

    const items = state.data[state.currentCategory] || [];
    const filteredItems = items.filter(item => {
        const name = (item.name || item.displayName || item.contactName || '').toLowerCase();
        return name.includes(state.searchQuery);
    });

    if (filteredItems.length === 0) {
        container.innerHTML = '<div class="empty-state" style="padding: 20px; font-size: 12px;">No items found in this category</div>';
        return;
    }

    if (state.currentCategory === 'messages') {
        renderMessageThreads(container, filteredItems);
    } else if (state.currentCategory === 'contacts') {
        renderContactList(container, filteredItems);
    } else {
        renderFileSidebar(container, filteredItems);
    }
}

function renderMessageThreads(container, threads) {
    threads.forEach(thread => {
        const div = document.createElement('div');
        div.className = `chat-thread ${state.selectedThreadId === thread.id ? 'selected' : ''}`;
        div.onclick = () => selectThread(thread);

        div.innerHTML = `
            <div class="avatar">${thread.contactName.charAt(0)}</div>
            <div class="thread-info">
                <div class="thread-name">${thread.contactName}</div>
                <div class="thread-preview">${thread.lastMessage || ''}</div>
            </div>
        `;
        container.appendChild(div);
    });
}

function renderContactList(container, contacts) {
    contacts.forEach(contact => {
        const div = document.createElement('div');
        div.className = `chat-thread ${state.selectedItem && state.selectedItem.id === contact.id ? 'selected' : ''}`;
        div.onclick = () => viewItem(contact);

        div.innerHTML = `
            <div class="avatar">${contact.name.charAt(0)}</div>
            <div class="thread-info">
                <div class="thread-name">${contact.name}</div>
                <div class="thread-preview">${contact.phones || contact.emails || ''}</div>
            </div>
        `;
        container.appendChild(div);
    });
}

function renderFileSidebar(container, files) {
    const list = document.createElement('div');
    list.className = 'item-list';

    files.forEach(file => {
        const div = document.createElement('div');
        div.className = `list-item ${state.selectedItem && state.selectedItem.path === file.path ? 'selected' : ''}`;
        div.onclick = () => viewItem(file);
        div.innerHTML = `
            <div class="thread-info">
                <div class="thread-name">${file.name}</div>
                <div class="thread-preview">${file.size || ''}</div>
            </div>
        `;
        list.appendChild(div);
    });
    container.appendChild(list);
}

function selectThread(thread) {
    state.selectedThreadId = thread.id;
    state.selectedItem = null;
    renderSidebar();
    renderMainContent();
}

function viewItem(item) {
    state.selectedItem = item;
    state.selectedThreadId = null;
    renderSidebar();
    renderMainContent();
}

function renderMainContent() {
    const display = document.getElementById('viewer-display');
    display.innerHTML = '';

    if (state.currentCategory === 'messages' && state.selectedThreadId) {
        renderConversation(display);
        return;
    }

    if (state.currentCategory === 'contacts' && state.selectedItem) {
        renderContactDetail(display, state.selectedItem);
        return;
    }

    if (state.selectedItem) {
        if (state.viewMode === 'grid' && ['pictures', 'videos'].includes(state.currentCategory)) {
            renderGrid(display);
        } else {
            renderFileViewer(display, state.selectedItem);
        }
        return;
    }

    // Default: If nothing selected, show grid for media categories
    if (['pictures', 'videos', 'audio'].includes(state.currentCategory)) {
        renderGrid(display);
    } else {
        display.innerHTML = '<div class="empty-state">Select an item from the sidebar to view</div>';
    }
}

function renderConversation(container) {
    const thread = state.data.messages.find(t => t.id === state.selectedThreadId);
    if (!thread) return;

    document.getElementById('current-title').textContent = `Conversation with ${thread.contactName}`;

    const chatContainer = document.createElement('div');
    chatContainer.className = 'item-gallery';

    thread.messages.forEach(msg => {
        const bubble = document.createElement('div');
        bubble.className = `message-bubble ${msg.type === 'incoming' ? 'message-incoming' : 'message-outgoing'}`;

        let content = `<div class="message-text">${msg.body}</div>`;

        if (msg.attachments && msg.attachments.length > 0) {
            msg.attachments.forEach(att => {
                if (att.mimeType.startsWith('image/')) {
                    content += `<img src="${att.path}" class="mms-attachment" onclick="Android.openFile('${att.path}')">`;
                } else if (att.mimeType.startsWith('video/')) {
                    content += `<video src="${att.path}" class="mms-attachment" controls></video>`;
                } else if (att.mimeType.startsWith('audio/')) {
                    content += `<audio src="${att.path}" class="mms-attachment" controls></audio>`;
                } else {
                    content += `<div class="list-item" onclick="Android.openFile('${att.path}')" style="background: rgba(255,255,255,0.1); margin-top: 8px; border-radius: 4px;">📎 ${att.fileName || 'Attachment'}</div>`;
                }
            });
        }

        bubble.innerHTML = `${content}<div class="message-time">${msg.timestamp}</div>`;
        chatContainer.appendChild(bubble);
    });

    container.appendChild(chatContainer);
}

function renderContactDetail(container, contact) {
    document.getElementById('current-title').textContent = contact.name;
    container.innerHTML = `
        <div class="item-gallery">
            <div class="avatar" style="width: 100px; height: 100px; font-size: 40px; margin: 0 auto 24px auto;">${contact.name.charAt(0)}</div>
            <div class="list-item">
                <div class="thread-info">
                    <div class="thread-preview">Name</div>
                    <div class="thread-name" style="font-size: 20px;">${contact.name}</div>
                </div>
            </div>
            <div class="list-item">
                <div class="thread-info">
                    <div class="thread-preview">Phone Numbers</div>
                    <div class="thread-name">${contact.phones || 'N/A'}</div>
                </div>
            </div>
            <div class="list-item">
                <div class="thread-info">
                    <div class="thread-preview">Emails</div>
                    <div class="thread-name">${contact.emails || 'N/A'}</div>
                </div>
            </div>
        </div>
    `;
}

function renderGrid(container) {
    const items = state.data[state.currentCategory] || [];

    if (items.length === 0) {
        container.innerHTML = '<div class="empty-state">No media files found in this category</div>';
        return;
    }

    const grid = document.createElement('div');
    grid.className = 'item-grid';

    // Performance Optimization: Virtual/Chunked rendering for large lists
    const CHUNK_SIZE = 50;
    let renderedCount = 0;

    function renderChunk() {
        const end = Math.min(renderedCount + CHUNK_SIZE, items.length);
        for (let i = renderedCount; i < end; i++) {
            const item = items[i];
            const div = document.createElement('div');
            div.className = `grid-item ${state.selectedItem && state.selectedItem.path === item.path ? 'selected' : ''}`;
            div.onclick = () => viewItem(item);

            if (state.currentCategory === 'pictures') {
                div.innerHTML = `<img src="${item.path}" loading="lazy" onerror="this.src='https://via.placeholder.com/150?text=Error'">`;
            } else if (state.currentCategory === 'videos') {
                div.innerHTML = `<video src="${item.path}#t=0.1" preload="metadata"></video><div class="video-icon">▶</div>`;
            } else {
                div.innerHTML = `<div class="empty-state" style="font-size: 10px;">${item.name}</div>`;
            }
            grid.appendChild(div);
        }
        renderedCount = end;
        if (renderedCount < items.length) {
            requestAnimationFrame(renderChunk);
        }
    }

    container.appendChild(grid);
    renderChunk();
}

function renderFileViewer(display, item) {
    const type = state.currentCategory;
    document.getElementById('current-title').textContent = item.name;

    if (type === 'pictures') {
        display.innerHTML = `<img src="${item.path}" class="media-preview">`;
    } else if (type === 'videos') {
        display.innerHTML = `<video src="${item.path}" class="media-preview" controls autoplay></video>`;
    } else if (type === 'audio') {
        display.innerHTML = `
            <div class="audio-player" style="text-align: center; padding: 40px;">
                <div class="avatar" style="width: 120px; height: 120px; margin: 0 auto 24px auto; background: var(--accent-color);">🎵</div>
                <h3>${item.name}</h3>
                <audio src="${item.path}" controls autoplay style="width: 100%; margin-top: 40px;"></audio>
            </div>`;
    } else if (type === 'documents') {
        display.innerHTML = `<embed src="${item.path}" type="${item.mimeType}">`;
    } else {
        display.innerHTML = `
            <div class="empty-state">
                <div>
                    <p style="margin-bottom: 8px;">File: ${item.name}</p>
                    <p style="font-size: 12px; color: var(--text-secondary); margin-bottom: 24px;">Size: ${item.size}</p>
                    <button onclick="Android.openFile('${item.path}')" style="padding: 12px 24px; background: var(--accent-color); border: none; border-radius: 8px; font-weight: bold; cursor: pointer; color: #000;">Open with External App</button>
                </div>
            </div>`;
    }
}

function loadMockData() {
    const mock = {
        pictures: [{name: 'photo1.jpg', path: 'https://via.placeholder.com/800x600', size: '2MB'}],
        videos: [],
        documents: [],
        messages: [{
            id: '1',
            contactName: 'John Doe',
            lastMessage: 'See you there!',
            messages: [
                {type: 'incoming', body: 'Hey, are we still on for today?', timestamp: 'Oct 25, 2023 10:00 AM', date: 1698220800000},
                {type: 'outgoing', body: 'Yes, absolutely!', timestamp: 'Oct 25, 2023 10:05 AM', date: 1698221100000},
                {type: 'incoming', body: 'Great! Check this out.', timestamp: 'Oct 25, 2023 10:10 AM', date: 1698221400000, attachments: [{mimeType: 'image/jpeg', path: 'https://via.placeholder.com/300', fileName: 'test.jpg'}]}
            ]
        }],
        contacts: [{id: 'c1', name: 'Alice Smith', phones: '555-0123', emails: 'alice@example.com'}],
        audio: [],
        other: []
    };
    loadData(mock);
}
