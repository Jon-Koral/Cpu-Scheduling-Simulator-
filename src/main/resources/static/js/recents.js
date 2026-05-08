import { processes, processColorMap, PROCESS_COLORS, resetProcesses, setProcessCounter } from './state.js'
import { renderProcessCard } from './modal.js'

const STORAGE_KEY = 'cpu-sim-recents'
const MAX_RECENTS = 20

let _run = null  // injected from main.js to avoid circular imports

// Init

export function initRecents(runFn) {
    _run = runFn
    document.getElementById('btn-hamburger').addEventListener('click', openSidebar)
    document.getElementById('recents-overlay').addEventListener('click', closeSidebar)
    document.getElementById('btn-close-recents').addEventListener('click', closeSidebar)
    document.getElementById('btn-clear-recents').addEventListener('click', clearRecents)
    renderRecents()
}

// Save

export function saveRecent(entry) {
    const recents = loadRecents()
    recents.unshift({ id: Date.now(), timestamp: new Date().toISOString(), ...entry })
    if (recents.length > MAX_RECENTS) recents.splice(MAX_RECENTS)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(recents))
    renderRecents()
}

// Replay

function replayRecent(r) {
    // 1. Wipe current processes from state and DOM
    resetProcesses()
    document.getElementById('process-list').innerHTML = ''

    // 2. Restore saved processes into state and re-render cards
    r.processes.forEach(p => {
        const num        = parseInt(p.processId.replace('P', ''))
        const colorIndex = ((num - 1) % PROCESS_COLORS.length) + 1
        processColorMap[p.processId] = colorIndex
        processes.push(p)
        renderProcessCard(p, colorIndex)
    })

    // 3. Advance the counter past the highest restored ID
    const maxNum = r.processes.reduce((max, p) => {
        return Math.max(max, parseInt(p.processId.replace('P', '')))
    }, 0)
    setProcessCounter(maxNum + 1)

    // 4. Restore mode and algorithm selection
    if (r.mode === 'single') {
        document.getElementById('btn-single').click()
        document.getElementById('algorithm').value = r.algorithm
        if (r.quantum) document.getElementById('quantum').value = r.quantum
        document.getElementById('algorithm').dispatchEvent(new Event('change'))
    } else {
        document.getElementById('btn-compare').click()
        document.querySelectorAll('#compare-controls input[type=checkbox]').forEach(cb => {
            cb.checked = r.algorithms.includes(cb.value)
        })
        if (r.quantum) document.getElementById('quantum').value = r.quantum
        document.querySelectorAll('#compare-controls input[type=checkbox]')[0]
            .dispatchEvent(new Event('change'))
    }

    // 5. Close sidebar and re-run
    closeSidebar()
    _run()
}

// Sidebar open / close

function openSidebar() {
    document.getElementById('recents-sidebar').classList.add('open')
    document.getElementById('recents-overlay').classList.add('active')
}

function closeSidebar() {
    document.getElementById('recents-sidebar').classList.remove('open')
    document.getElementById('recents-overlay').classList.remove('active')
}

// Storage helpers

function loadRecents() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [] }
    catch { return [] }
}

function clearRecents() {
    localStorage.removeItem(STORAGE_KEY)
    renderRecents()
}

// Render

function timeAgo(iso) {
    const diff = Date.now() - new Date(iso).getTime()
    const mins = Math.floor(diff / 60000)
    const hrs  = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    if (mins < 1)  return 'Just now'
    if (mins < 60) return `${mins}m ago`
    if (hrs  < 24) return `${hrs}h ago`
    return `${days}d ago`
}

function renderRecents() {
    const list    = document.getElementById('recents-list')
    const recents = loadRecents()

    if (recents.length === 0) {
        list.innerHTML = `<p class="recents-empty">No recent runs yet.<br>Run an algorithm to see it here.</p>`
        return
    }

    list.innerHTML = recents.map((r, idx) => {
        const label = r.mode === 'compare'
            ? r.algorithms.join(' · ')
            : r.algorithm + (r.quantum ? ` (q=${r.quantum})` : '')

        const statPills = r.mode === 'single' ? `
            <div class="recent-stats">
                <span class="recent-pill">Wait&nbsp;<strong>${r.avgWaitingTime.toFixed(2)}</strong></span>
                <span class="recent-pill">TAT&nbsp;<strong>${r.avgTurnaroundTime.toFixed(2)}</strong></span>
                <span class="recent-pill">Thru&nbsp;<strong>${r.throughput.toFixed(3)}</strong></span>
            </div>` : ''

        return `
            <div class="recent-entry" data-idx="${idx}">
                <div class="recent-top">
                    <span class="recent-algo">${label}</span>
                    <span class="recent-time">${timeAgo(r.timestamp)}</span>
                </div>
                <div class="recent-meta">
                    <span class="recent-tag">${r.mode === 'compare' ? 'Compare' : 'Single'}</span>
                    ${r.processCount} process${r.processCount !== 1 ? 'es' : ''}
                </div>
                ${statPills}
                <div class="recent-replay-hint">▶ Click to replay</div>
            </div>`
    }).join('')

    // attach click listeners after innerHTML is set
    list.querySelectorAll('.recent-entry').forEach(el => {
        el.addEventListener('click', () => {
            replayRecent(recents[parseInt(el.dataset.idx)])
        })
    })
}