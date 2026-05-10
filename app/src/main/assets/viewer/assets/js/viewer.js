let currentPath = [];
let manifest = null;
let contacts = null;
let theme = {
    accent: '#0084ff',
    bg: '#1a1a1a',
    text: '#ffffff'
};

let selectionMode = false;
let selectedIds = new Set();
let currentCategory = '';

document.addEventListener('DOMContentLoaded', () => {
    applyThemeFromUrl();
    init();
});

function applyThemeFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('accent')) theme.accent = params.get('accent');
    if (params.has('bg')) theme.bg = params.get('bg');
    if (params.has('text')) theme.text = params.get('text');

    document.documentElement.style.setProperty('--primary', theme.accent);
    document.documentElement.style.setProperty('--bg', theme.bg);
    document.documentElement.style.setProperty('--text', theme.text);
    document.documentElement.style.setProperty('--sidebar-bg', 'rgba(255,255,255,0.05)');
    document.documentElement.style.setProperty('--msg-in', 'rgba(255,255,255,0.1)');
    document.documentElement.style.setProperty('--msg-out', theme.accent + '44');
    document.documentElement.style.setProperty('--accent-alpha', theme.accent + '22');
}

async function init() {
    document.getElementById('back-button').onclick = () => navigateBack();
    document.getElementById('home-button').onclick = () => loadHome();
    document.getElementById('import-button').onclick = () => {
        if (window.Android) Android.importBackup();
    };
    document.getElementById('export-button').onclick = () => {
        if (window.Android) Android.exportArchive();
    };
    document.getElementById('exit-button').onclick = () => {
        if (window.Android) Android.exitViewer();
    };

    // Setup Quick Nav
    document.querySelectorAll('.nav-links a').forEach(link => {
        link.onclick = (e) => {
            e.preventDefault();
            const section = link.getAttribute('data-section');
            exitSelectionMode();
            switch(section) {
                case 'sms': navigateTo('sms_list'); break;
                case 'calls': navigateTo('calls'); break;
                case 'contacts': navigateTo('contacts'); break;
                case 'images': navigateTo('gallery', 'Pictures'); break;
                case 'videos': navigateTo('gallery', 'Videos'); break;
                case 'files': navigateTo('files'); break;
            }
        };
    });

    try {
        const res = await fetch('../../data/contacts.json');
        contacts = await res.json();
    } catch (e) { console.log("No contacts loaded"); }

    // Restore state if available
    const savedPath = localStorage.getItem('rts_viewer_path');
    if (savedPath) {
        try {
            currentPath = JSON.parse(savedPath);
            const last = currentPath[currentPath.length - 1];
            renderPage(last.type, last.data);
            updateHeader(getTitleFromPath(last), currentPath.length > 1);
        } catch (e) {
            loadHome();
        }
    } else {
        loadHome();
    }
}

function updateHeader(title, showBack = false) {
    document.getElementById('header-title').innerText = selectionMode ? `${selectedIds.size} Selected` : title;
    document.getElementById('back-button').style.display = showBack ? 'flex' : 'none';
    document.getElementById('home-button').style.display = showBack ? 'flex' : 'none';

    // Update active state in quick nav
    const type = currentPath.length > 0 ? currentPath[currentPath.length-1].type : '';
    const sectionData = currentPath.length > 0 ? currentPath[currentPath.length-1].data : '';

    document.querySelectorAll('.nav-links a').forEach(link => {
        const section = link.getAttribute('data-section');
        let isActive = false;
        if (section === 'sms') isActive = (type === 'sms_list' || type === 'conversation');
        else if (section === 'calls') isActive = (type === 'calls');
        else if (section === 'contacts') isActive = (type === 'contacts');
        else if (section === 'images') isActive = (type === 'gallery' && sectionData === 'Pictures');
        else if (section === 'videos') isActive = (type === 'gallery' && sectionData === 'Videos');
        else if (section === 'files') isActive = (type === 'files');

        link.classList.toggle('active', isActive);
    });
}

function navigateBack() {
    exitSelectionMode();
    if (currentPath.length > 1) {
        currentPath.pop();
        saveState();
        const prev = currentPath[currentPath.length - 1];
        renderPage(prev.type, prev.data);
    } else {
        loadHome();
    }
}

function renderPage(type, data = null) {
    const main = document.getElementById('main-content');
    main.innerHTML = '';

    // Reset selection state when rendering new page
    if (!selectionMode) {
        selectedIds.clear();
    }

    switch(type) {
        case 'home': renderHome(main); break;
        case 'sms_list': renderSmsList(main); break;
        case 'conversation': renderConversation(main, data); break;
        case 'calls': renderCallLogs(main); break;
        case 'contacts': renderContacts(main); break;
        case 'gallery': renderGallery(main, data); break;
        case 'apks': renderApks(main); break;
        case 'files': renderFiles(main); break;
    }
}

function loadHome() {
    exitSelectionMode();
    currentPath = [{type: 'home'}];
    saveState();
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
        { id: 'files', label: 'Files', icon: '📁' },
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
            else if (item.id === 'files') navigateTo('files');
            else if (item.id === 'apks') navigateTo('apks');
        };
        grid.appendChild(div);
    });
    container.appendChild(grid);
}

function navigateTo(type, data = null) {
    exitSelectionMode();
    const sections = ['sms_list', 'calls', 'contacts', 'gallery', 'files', 'apks'];
    if (sections.includes(type) && currentPath.length > 1 && sections.includes(currentPath[currentPath.length-1].type)) {
        currentPath[currentPath.length-1] = {type, data};
    } else {
        currentPath.push({type, data});
    }
    saveState();
    renderPage(type, data);
}

function saveState() {
    localStorage.setItem('rts_viewer_path', JSON.stringify(currentPath));
}

function getContactInfo(phone) {
    if (!contacts || !phone) return null;
    const cleanPhone = phone.replace(/[\s\-\(\)]/g, '');
    return contacts.find(c => {
        return c.phoneNumbers.some(p => {
            const cp = p.replace(/[\s\-\(\)]/g, '');
            return cp.endsWith(cleanPhone) || cleanPhone.endsWith(cp);
        });
    });
}

function getAvatarHtml(name, phone) {
    const contact = getContactInfo(phone);
    const displayName = contact ? contact.name : (name || phone || '?');
    const initials = displayName.charAt(0).toUpperCase();
    if (contact && contact.photoUri) {
        return `<div class="contact-avatar"><img src="${contact.photoUri}" onerror="this.style.display='none'; this.parentElement.innerHTML='${initials}'"></div>`;
    }
    return `<div class="contact-avatar">${initials}</div>`;
}

function enterSelectionMode(category) {
    selectionMode = true;
    currentCategory = category;
    document.body.classList.add('selection-active');
    updateSelectionUI();
}

function exitSelectionMode() {
    selectionMode = false;
    selectedIds.clear();
    document.body.classList.remove('selection-active');
    const bar = document.getElementById('selection-bar');
    if (bar) bar.remove();

    const title = currentPath.length > 0 ? getTitleFromPath(currentPath[currentPath.length-1]) : 'RTS Backup Suite';
    updateHeader(title, currentPath.length > 1);

    // Refresh items to remove checkbox UI
    const items = document.querySelectorAll('.item-row, .gallery-item');
    items.forEach(it => it.classList.remove('selected'));
}

function getTitleFromPath(path) {
    switch(path.type) {
        case 'sms_list': return 'Messages';
        case 'calls': return 'Call Logs';
        case 'contacts': return 'Contacts';
        case 'gallery': return path.data;
        case 'files': return 'Files';
        case 'apks': return 'APKs';
        default: return 'RTS Backup Suite';
    }
}

function toggleSelection(id, element) {
    if (selectedIds.has(id)) {
        selectedIds.delete(id);
        element.classList.remove('selected');
    } else {
        selectedIds.add(id);
        element.classList.add('selected');
    }

    if (selectedIds.size === 0) {
        exitSelectionMode();
    } else {
        updateSelectionUI();
    }
}

function updateSelectionUI() {
    updateHeader(`${selectedIds.size} Selected`, true);
    let bar = document.getElementById('selection-bar');
    if (!bar) {
        bar = document.createElement('div');
        bar.id = 'selection-bar';
        bar.innerHTML = `
            <button onclick="exitSelectionMode()">Cancel</button>
            <div style="flex:1"></div>
            <button class="delete-selected" onclick="performMassDelete()">Delete Selected</button>
        `;
        document.getElementById('app').appendChild(bar);
    }
}

async function performMassDelete() {
    if (selectedIds.size === 0) return;
    if (!confirm(`Delete ${selectedIds.size} items?`)) return;

    const ids = Array.from(selectedIds);
    if (window.Android) {
        Android.deleteItems(currentCategory, JSON.stringify(ids));
    } else {
        // Handle deletion in exported version via local server
        try {
            for (const id of ids) {
                let url = '';
                if (['Pictures', 'Videos', 'UserFile', 'Apk'].includes(currentCategory)) {
                     const entry = manifest.entries.find(e => e.identifier === id);
                     if (entry) url = `../../internal/${entry.filePath}`;
                } else if (currentCategory === 'sms') {
                     url = `../../data/threads/${id}.json`;
                }

                if (url) {
                    await fetch(url, { method: 'DELETE' });
                }
            }
            // Simple approach: reload the page to show what's left
            window.location.reload();
        } catch (e) {
            alert('Deletion not supported in this environment or failed.');
        }
    }
    exitSelectionMode();
}

function wrapItem(element, id, category) {
    element.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        if (!selectionMode) enterSelectionMode(category);
        toggleSelection(id, element);
    });

    element.addEventListener('click', (e) => {
        if (selectionMode) {
            e.stopPropagation();
            toggleSelection(id, element);
        }
    });

    if (selectionMode && selectedIds.has(id)) {
        element.classList.add('selected');
    }
}

async function renderSmsList(container) {
    updateHeader('Messages', true);
    addMassDelete(container, 'sms');
    try {
        const response = await fetch('../../data/index.json');
        const threads = await response.json();

        const list = document.createElement('div');
        list.className = 'list-container';

        threads.forEach(thread => {
            const contact = getContactInfo(thread.phone);
            const name = contact ? contact.name : (thread.contact || thread.phone);
            const row = document.createElement('div');
            row.className = 'item-row';
            row.innerHTML = `
                <div class="selection-check"></div>
                ${getAvatarHtml(name, thread.phone)}
                <div class="row-content">
                    <div class="title">${name}</div>
                    <div class="subtitle">${thread.last_message}</div>
                    <div class="meta">${new Date(thread.timestamp).toLocaleString()}</div>
                </div>
                <div class="row-delete" onclick="event.stopPropagation(); deleteItem('sms', '${thread.thread_id}')">🗑️</div>
            `;
            wrapItem(row, thread.thread_id, 'sms');
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) { container.innerHTML = '<p class="placeholder">No SMS data found.</p>'; }
}

async function renderConversation(container, thread) {
    const contact = getContactInfo(thread.phone);
    const name = contact ? contact.name : (thread.contact || thread.phone);
    updateHeader(name, true);

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
                        content += `<img src="${attPath}" class="mms-image" onclick="viewFull('${attPath}', 'image', 'sms', '${thread.thread_id}')">`;
                    } else if (att.contentType.startsWith('video/')) {
                        content += `<video src="${attPath}" controls class="mms-video"></video>`;
                    } else if (att.contentType.startsWith('audio/')) {
                        content += `<audio src="${attPath}" controls class="mms-audio"></audio>`;
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
    } catch (e) { container.innerHTML = '<p class="placeholder">Failed to load conversation.</p>'; }
}

async function renderCallLogs(container) {
    updateHeader('Call Logs', true);
    addMassDelete(container, 'calls');
    try {
        const response = await fetch('../../data/calls.json');
        const calls = await response.json();
        const list = document.createElement('div');
        list.className = 'list-container';

        calls.forEach(call => {
            const contact = getContactInfo(call.number);
            const name = contact ? contact.name : call.number;
            const row = document.createElement('div');
            row.className = 'item-row';
            const typeLabel = call.latestType === "1" ? "Incoming" : call.latestType === "2" ? "Outgoing" : "Missed";
            row.innerHTML = `
                <div class="selection-check"></div>
                ${getAvatarHtml(name, call.number)}
                <div class="row-content">
                    <div class="title">${name}</div>
                    <div class="subtitle">${typeLabel} • ${call.totalDuration}s</div>
                    <div class="meta">${new Date(call.date).toLocaleString()}</div>
                </div>
                <div class="row-delete" onclick="event.stopPropagation(); deleteItem('calls', '${call.id}')">🗑️</div>
            `;
            wrapItem(row, call.id, 'calls');
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) { container.innerHTML = '<p class="placeholder">No Call Logs found.</p>'; }
}

async function renderContacts(container) {
    updateHeader('Contacts', true);
    addMassDelete(container, 'contacts');
    try {
        const list = document.createElement('div');
        list.className = 'list-container';

        contacts.sort((a,b) => a.name.localeCompare(b.name)).forEach(contact => {
            const row = document.createElement('div');
            row.className = 'item-row';
            row.innerHTML = `
                <div class="selection-check"></div>
                ${getAvatarHtml(contact.name, contact.phoneNumbers[0] || '')}
                <div class="row-content">
                    <div class="title">${contact.name}</div>
                    <div class="subtitle">${contact.phoneNumbers.join(', ') || 'No number'}</div>
                </div>
                <div class="row-delete" onclick="event.stopPropagation(); deleteItem('contacts', '${contact.id}')">🗑️</div>
            `;
            wrapItem(row, contact.id, 'contacts');
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) { container.innerHTML = '<p class="placeholder">No Contacts found.</p>'; }
}

async function renderGallery(container, category) {
    updateHeader(category, true);
    addMassDelete(container, category);
    try {
        if (!manifest) {
            const res = await fetch('../../internal/manifest.json');
            manifest = await res.json();
        }

        const files = manifest.entries.filter(e => e.category === category);
        if (files.length === 0) {
            container.innerHTML = '<p class="placeholder">No items in this category.</p>';
            return;
        }

        const gallery = document.createElement('div');
        gallery.className = 'gallery';

        files.forEach(file => {
            const item = document.createElement('div');
            item.className = 'gallery-item';
            const path = `../../internal/${file.filePath}`;

            let content = '';
            if (category === 'Videos' || file.itemType === 'Video') {
                content = `<video src="${path}" muted></video><div class="play-icon">▶</div>`;
            } else {
                content = `<img src="${path}">`;
            }

            item.innerHTML = `
                <div class="selection-check-gallery"></div>
                ${content}
                <div class="delete-btn" onclick="event.stopPropagation(); deleteItem('${category}', '${file.identifier}')">✕</div>
            `;

            item.onclick = (e) => {
                if (selectionMode) {
                    toggleSelection(file.identifier, item);
                } else {
                    if (category === 'Videos' || file.itemType === 'Video') viewFull(path, 'video', category, file.identifier);
                    else viewFull(path, 'image', category, file.identifier);
                }
            };

            wrapItem(item, file.identifier, category);
            gallery.appendChild(item);
        });
        container.appendChild(gallery);
    } catch (e) { container.innerHTML = '<p class="placeholder">Failed to load gallery.</p>'; }
}

async function renderFiles(container) {
    updateHeader('Restored Files', true);
    addMassDelete(container, 'files');
    try {
        if (!manifest) {
            const res = await fetch('../../internal/manifest.json');
            manifest = await res.json();
        }

        const files = manifest.entries.filter(e => e.itemType === 'UserFile');
        if (files.length === 0) {
            container.innerHTML = '<p class="placeholder">No restored files found.</p>';
            return;
        }

        const gallery = document.createElement('div');
        gallery.className = 'gallery';

        files.forEach(file => {
            const item = document.createElement('div');
            item.className = 'gallery-item';
            const path = `../../internal/${file.filePath}`;
            const ext = file.itemName.split('.').pop().toLowerCase();
            const isImage = ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext);

            item.innerHTML = `
                <div class="selection-check-gallery"></div>
                ${isImage ? `<img src="${path}">` : `<div class="file-card"><div class="file-icon">📄</div></div>`}
                <div class="item-label">${file.itemName}</div>
                <div class="delete-btn" onclick="event.stopPropagation(); deleteItem('files', '${file.identifier}')">✕</div>
            `;

            item.onclick = () => {
                if (selectionMode) {
                    toggleSelection(file.identifier, item);
                } else {
                    isImage ? viewFull(path, 'image', 'files', file.identifier) : window.open(path, '_blank');
                }
            };

            wrapItem(item, file.identifier, 'files');
            gallery.appendChild(item);
        });
        container.appendChild(gallery);
    } catch (e) { container.innerHTML = '<p class="placeholder">Failed to load files.</p>'; }
}

function addMassDelete(container, category) {
    const bar = document.createElement('div');
    bar.className = 'section-actions';
    bar.innerHTML = `
        <button class="mass-delete-btn" onclick="deleteCategory('${category}')">DELETE ALL SECTION</button>
        <button class="mass-delete-btn" style="border-color:var(--primary); color:var(--primary); margin-left:8px;" onclick="enterSelectionMode('${category}')">SELECT ITEMS</button>
    `;
    container.appendChild(bar);
}

function deleteItem(cat, id) {
    if (confirm('Delete this item?')) {
        if (window.Android) {
            Android.deleteItem(cat, id);
        } else {
             // Handle local deletion for exported version
             performMassDeleteLocal(cat, [id]);
        }
    }
}

async function performMassDeleteLocal(cat, ids) {
    try {
        for (const id of ids) {
            let url = '';
            if (['Pictures', 'Videos', 'UserFile', 'Apk'].includes(cat)) {
                const entry = manifest.entries.find(e => e.identifier === id);
                if (entry) url = `../../internal/${entry.filePath}`;
            } else if (cat === 'sms') {
                url = `../../data/threads/${id}.json`;
            }
            if (url) await fetch(url, { method: 'DELETE' });
        }
        window.location.reload();
    } catch(e) { console.error(e); }
}

function deleteCategory(cat) {
    if (confirm(`ARE YOU SURE? This will permanently delete ALL content in ${cat}!`)) {
        if (window.Android) Android.deleteCategory(cat);
        else alert('Full category deletion not available in exported version.');
    }
}

function viewFull(path, type, cat = 'generic', id = null) {
    if (selectionMode) return;
    const viewer = document.createElement('div');
    viewer.className = 'fullscreen-viewer';
    const content = type === 'video' ? `<video src="${path}" controls autoplay></video>` : `<img src="${path}">`;
    const deleteHtml = id ? `<div class="delete-btn" style="top:20px; right:80px;" onclick="document.body.removeChild(this.parentElement); deleteItem('${cat}', '${id}')">🗑️</div>` : '';
    viewer.innerHTML = `
        <div class="viewer-close">&times;</div>
        ${content}
        ${deleteHtml}
        <div class="viewer-download" onclick="window.open('${path}', '_blank')">Save File</div>
    `;
    viewer.onclick = (e) => {
        if (e.target.className === 'fullscreen-viewer' || e.target.className === 'viewer-close') {
            document.body.removeChild(viewer);
        }
    };
    document.body.appendChild(viewer);
}

async function renderApks(container) {
    updateHeader('Installed APKs', true);
    addMassDelete(container, 'apks');
    try {
        if (!manifest) {
            const res = await fetch('../../internal/manifest.json');
            manifest = await res.json();
        }
        const apks = manifest.entries.filter(e => e.itemType === 'Apk');
        const list = document.createElement('div');
        list.className = 'list-container';

        apks.forEach(apk => {
            const row = document.createElement('div');
            row.className = 'item-row';
            row.innerHTML = `
                <div class="selection-check"></div>
                <div class="contact-avatar">📦</div>
                <div class="row-content">
                    <div class="title">${apk.itemName}</div>
                    <div class="subtitle">${apk.identifier}</div>
                </div>
                <div class="row-delete" onclick="event.stopPropagation(); deleteItem('apks', '${apk.identifier}')">🗑️</div>
            `;
            wrapItem(row, apk.identifier, 'apks');
            list.appendChild(row);
        });
        container.appendChild(list);
    } catch (e) { container.innerHTML = '<p class="placeholder">No APKs found.</p>'; }
}
