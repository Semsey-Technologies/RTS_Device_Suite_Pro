let currentPath = []; // For navigation stack
let manifest = null;

document.addEventListener('DOMContentLoaded', () => {
    init();
});

async function init() {
    const backBtn = document.getElementById('back-button');
    const homeBtn = document.getElementById('home-button');

    backBtn.onclick = () => navigateBack();
    homeBtn.onclick = () => loadHome();

    loadHome();
}

function updateHeader(title, showBack = false) {
    document.getElementById('header-title').innerText = title;
    document.getElementById('back-button').style.display = showBack ? 'block' : 'none';
    document.getElementById('home-button').style.display = showBack ? 'block' : 'none';
}

function navigateBack() {
    if (currentPath.length > 1) {
        currentPath.pop();
        const prev = currentPath[currentPath.length - 1];
        renderPage(prev.type, prev.data);
    } else {
        loadHome();
    }
}

function renderPage(type, data = null) {
    const main = document.getElementById('main-content');
    main.innerHTML = '';

    // Save to path if not already there (simple check)
    if (currentPath.length === 0 || currentPath[currentPath.length-1].type !== type) {
        // This is a simplified nav for the demo
    }

    switch(type) {
        case 'home':
            renderHome(main);
            break;
        case 'sms_list':
            renderSmsList(main);
            break;
        case 'conversation':
            renderConversation(main, data);
            break;
        case 'calls':
            renderCallLogs(main);
            break;
        case 'contacts':
            renderContacts(main);
            break;
        case 'gallery':
            renderGallery(main, data); // data = category (Images, Videos)
            break;
        case 'apks':
            renderApks(main);
            break;
    }
}

function loadHome() {
    currentPath = [{type: 'home'}];
    renderPage('home');
    updateHeader('RTS Backup Suite', false);
}

function renderHome(container) {
    const menu = [
        { id: 'sms', label: 'SMS/MMS', icon: '💬' },
        { id: 'calls', label: 'Call Logs', icon: '📞' },
        { id: 'contacts', label: 'Contacts', icon: '👤' },
        { id: 'images', label: 'Images', icon: '🖼️' },
        { id: 'videos', label: 'Videos', icon: '🎥' },
        { id: 'audio', label: 'Audio', icon: '🎵' },
        { id: 'apks', label: 'APKs', icon: '📦' }
    ];

    const grid = document.createElement('div');
    grid.className = 'menu-grid';

    menu.forEach(item => {
        const div = document.createElement('div');
        div.className = 'menu-item';
        div.innerHTML = `<i>${item.icon}</i><span>${item.label}</span>`;
        div.onclick = () => {
            if (item.id === 'sms') navigateTo('sms_list');
            else if (item.id === 'calls') navigateTo('calls');
            else if (item.id === 'contacts') navigateTo('contacts');
            else if (item.id === 'images') navigateTo('gallery', 'Pictures');
            else if (item.id === 'videos') navigateTo('gallery', 'Videos');
            else if (item.id === 'audio') navigateTo('gallery', 'Audio');
            else if (item.id === 'apks') navigateTo('apks');
        };
        grid.appendChild(div);
    });
    container.appendChild(grid);
}

function navigateTo(type, data = null) {
    currentPath.push({type, data});
    renderPage(type, data);
}

async function renderSmsList(container) {
    updateHeader('Messages', true);
    try {
        const response = await fetch('../../data/index.json');
        const threads = await response.json();

        const list = document.createElement('div');
        list.className = 'list-container';

        threads.forEach(thread => {
            const row = document.createElement('div');
            row.className = 'item-row';
            row.innerHTML = `
                <div class="title">${thread.contact || thread.phone}</div>
                <div class="subtitle">${thread.last_message}</div>
                <div class="meta">${new Date(thread.timestamp).toLocaleString()}</div>
            `;
            row.onclick = () => navigateTo('conversation', thread);
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) {
        container.innerHTML = '<p class="placeholder">No SMS data found or failed to load.</p>';
    }
}

async function renderConversation(container, thread) {
    updateHeader(thread.contact || thread.phone, true);
    try {
        const response = await fetch(`../../data/threads/${thread.thread_id}.json`);
        const messages = await response.json();

        const chat = document.createElement('div');
        chat.className = 'chat-container';

        messages.forEach(msg => {
            const div = document.createElement('div');
            div.className = `message ${msg.type === 2 ? 'outgoing' : 'incoming'}`;

            let content = `<p>${msg.body}</p>`;
            if (msg.attachments && msg.attachments.length > 0) {
                msg.attachments.forEach(att => {
                    const attPath = `../../data/mms/${thread.thread_id}/${att.fileName}`;
                    if (att.contentType.startsWith('image/')) {
                        content += `<img src="${attPath}" class="mms-image">`;
                    } else if (att.contentType.startsWith('video/')) {
                        content += `<video src="${attPath}" controls class="mms-video" style="max-width:100%; border-radius:8px;"></video>`;
                    } else if (att.contentType.startsWith('audio/')) {
                        content += `<audio src="${attPath}" controls class="mms-audio" style="width:100%;"></audio>`;
                    } else {
                        content += `<div class="attachment-link">📎 ${att.fileName}</div>`;
                    }
                });
            }
            content += `<div class="meta" style="font-size: 0.7rem; text-align: right; opacity: 0.7;">${new Date(msg.date).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</div>`;

            div.innerHTML = content;
            chat.appendChild(div);
        });
        container.appendChild(chat);
        window.scrollTo(0, document.body.scrollHeight);
    } catch (e) {
        container.innerHTML = '<p class="placeholder">Failed to load conversation.</p>';
    }
}

async function renderCallLogs(container) {
    updateHeader('Call Logs', true);
    try {
        const response = await fetch('../../data/calls.json');
        const calls = await response.json();
        const list = document.createElement('div');
        list.className = 'list-container';

        calls.forEach(call => {
            const row = document.createElement('div');
            row.className = 'item-row';
            const type = call.latestType === "1" ? "Incoming" : call.latestType === "2" ? "Outgoing" : "Missed";
            row.innerHTML = `
                <div class="title">${call.number}</div>
                <div class="subtitle">${type} • ${call.totalDuration}s</div>
                <div class="meta">${new Date(call.date).toLocaleString()}</div>
            `;
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) {
        container.innerHTML = '<p class="placeholder">No Call Logs found.</p>';
    }
}

async function renderContacts(container) {
    updateHeader('Contacts', true);
    try {
        const response = await fetch('../../data/contacts.json');
        const contacts = await response.json();
        const list = document.createElement('div');
        list.className = 'list-container';

        contacts.sort((a,b) => a.name.localeCompare(b.name)).forEach(contact => {
            const row = document.createElement('div');
            row.className = 'item-row';
            row.innerHTML = `
                <div class="title">${contact.name}</div>
                <div class="subtitle">${contact.phoneNumbers.join(', ') || 'No number'}</div>
                <div class="meta">${contact.emails.join(', ') || ''}</div>
            `;
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) {
        container.innerHTML = '<p class="placeholder">No Contacts found.</p>';
    }
}

async function renderGallery(container, category) {
    updateHeader(category, true);
    try {
        if (!manifest) {
            const res = await fetch('../../manifest.json');
            manifest = await res.json();
        }

        const files = manifest.entries.filter(e => e.category === category);
        const gallery = document.createElement('div');
        gallery.className = 'gallery';

        files.forEach(file => {
            const item = document.createElement('div');
            item.className = 'gallery-item';

            const path = `../../${file.filePath}`;
            if (category === 'Videos') {
                item.innerHTML = `<video src="${path}" muted></video>`;
            } else if (category === 'Pictures') {
                item.innerHTML = `<img src="${path}">`;
            } else {
                item.innerHTML = `<div style="padding: 10px; font-size: 0.8rem;">${file.itemName}</div>`;
            }

            // Simple fullscreen toggle on click
            item.onclick = () => {
                const viewer = document.createElement('div');
                viewer.style = "position:fixed; top:0; left:0; width:100%; height:100%; background:black; z-index:1000; display:flex; align-items:center; justify-content:center;";
                const content = category === 'Videos' ? `<video src="${path}" controls autoplay style="max-width:100%; max-height:100%;"></video>` : `<img src="${path}" style="max-width:100%; max-height:100%;">`;
                viewer.innerHTML = content;
                viewer.onclick = () => document.body.removeChild(viewer);
                document.body.appendChild(viewer);
            };

            gallery.appendChild(item);
        });
        container.appendChild(gallery);
    } catch (e) {
        container.innerHTML = '<p class="placeholder">Failed to load gallery.</p>';
    }
}

async function renderApks(container) {
    updateHeader('Installed APKs', true);
    try {
        if (!manifest) {
            const res = await fetch('../../manifest.json');
            manifest = await res.json();
        }
        const apks = manifest.entries.filter(e => e.itemType === 'Apk');
        const list = document.createElement('div');
        list.className = 'list-container';

        apks.forEach(apk => {
            const row = document.createElement('div');
            row.className = 'item-row';
            row.innerHTML = `
                <div class="title">${apk.itemName}</div>
                <div class="subtitle">${apk.identifier}</div>
            `;
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) {
        container.innerHTML = '<p class="placeholder">No APKs found.</p>';
    }
}
