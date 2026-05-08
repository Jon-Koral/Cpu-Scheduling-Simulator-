const processes = []
let processCounter = 1
let currentMode = 'single'   // 'single' | 'compare'

const API_BASE = 'http://localhost:8080/api'

const PROCESS_COLORS = [
    'indianred',
    'peru',
    'goldenrod',
    'mediumseagreen',
    'steelblue',
    'slateblue',
    'mediumvioletred',
    'lightseagreen',
]
const processColorMap = {}

function getColorIndex(processId) {
    return processColorMap[processId] || 1
}
function getColor(processId) {
    return PROCESS_COLORS[(getColorIndex(processId) - 1) % PROCESS_COLORS.length]
}

// ─── Mode toggle ─────────────────────────────────────────────────────────────

function setMode(mode) {
    currentMode = mode

    document.getElementById('single-controls').style.display  = mode === 'single'  ? 'flex' : 'none'
    document.getElementById('compare-controls').style.display = mode === 'compare' ? 'flex' : 'none'

    document.getElementById('btn-single').classList.toggle('mode-btn-active',  mode === 'single')
    document.getElementById('btn-compare').classList.toggle('mode-btn-active', mode === 'compare')

    // hide both result panels when switching modes
    document.getElementById('results').style.display            = 'none'
    document.getElementById('comparison-results').style.display = 'none'

    updateQuantumVisibility()
}

// show quantum input when RR is selected in either mode
function updateQuantumVisibility() {
    const quantumGroup = document.getElementById('quantum-group')

    if (currentMode === 'single') {
        const algo = document.getElementById('algorithm').value
        quantumGroup.classList.toggle('visible', algo === 'RR')
    } else {
        const checkboxes = document.querySelectorAll('#compare-controls input[type=checkbox]')
        const rrChecked  = Array.from(checkboxes).some(cb => cb.value === 'RR' && cb.checked)
        quantumGroup.classList.toggle('visible', rrChecked)
    }
}

function handleAlgorithmChange() {
    updateQuantumVisibility()
}

// attach change listeners to all compare checkboxes
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('#compare-controls input[type=checkbox]').forEach(cb => {
        cb.addEventListener('change', updateQuantumVisibility)
    })
    setMode('single')
})

// ─── Modal ───────────────────────────────────────────────────────────────────

function openModal() {
    document.getElementById('modal').classList.add('active')
    document.getElementById('arrival-time').focus()
    document.getElementById('modal-error').textContent = ''
}

function closeModal() {
    document.getElementById('modal').classList.remove('active')
    document.getElementById('arrival-time').value = ''
    document.getElementById('burst-time').value   = ''
    document.getElementById('priority').value     = ''
    document.getElementById('modal-error').textContent = ''
}

function handleOverlayClick(e) {
    if (e.target === document.getElementById('modal')) closeModal()
}

function addProcess() {
    const arrivalTime = document.getElementById('arrival-time').value
    const burstTime   = document.getElementById('burst-time').value
    const priority    = document.getElementById('priority').value

    if (arrivalTime === '' || burstTime === '') {
        document.getElementById('modal-error').textContent = 'Arrival Time or Burst Time is required.'
        return
    }

    const processId  = 'P' + processCounter
    const colorIndex = ((processCounter - 1) % PROCESS_COLORS.length) + 1
    processColorMap[processId] = colorIndex

    const process = {
        processId:   processId,
        arrivalTime: Number(arrivalTime),
        burstTime:   Number(burstTime),
        priority:    priority !== '' ? Number(priority) : 0
    }

    processes.push(process)
    processCounter++
    renderProcessCard(process, colorIndex)
    closeModal()
}

function renderProcessCard(process, colorIndex) {
    const idx  = colorIndex || getColorIndex(process.processId)
    const card = document.createElement('div')
    card.className = `process-card pc-${idx}`
    card.id = 'card-' + process.processId
    card.innerHTML = `
        <div class="p-badge">${process.processId}</div>
        <div class="p-info">
            <div class="p-field">
                <span class="p-label">Arrival</span>
                <span class="p-value">${process.arrivalTime}</span>
            </div>
            <div class="p-field">
                <span class="p-label">Burst</span>
                <span class="p-value">${process.burstTime}</span>
            </div>
            <div class="p-field">
                <span class="p-label">Priority</span>
                <span class="p-value">${process.priority}</span>
            </div>
        </div>
        <button class="btn-delete" onclick="deleteProcess('${process.processId}')">Delete</button>
    `
    document.getElementById('process-list').appendChild(card)
}

function deleteProcess(processId) {
    const index = processes.findIndex(p => p.processId === processId)
    if (index !== -1) processes.splice(index, 1)
    const card = document.getElementById('card-' + processId)
    if (card) card.remove()
}

// ─── Run (dispatches to single or compare) ───────────────────────────────────

async function run() {
    if (processes.length === 0) {
        alert('Please add at least one process first.')
        return
    }
    if (currentMode === 'single') {
        await runAlgorithm()
    } else {
        await runComparison()
    }
}

// ─── Single mode ─────────────────────────────────────────────────────────────

async function runAlgorithm() {
    const algorithm = document.getElementById('algorithm').value
    const quantum   = algorithm === 'RR' ? Number(document.getElementById('quantum').value) : null

    const requestBody = { algorithm, quantum, processes }
    console.log('Sending JSON to the backend:', requestBody)

    try {
        const response = await fetch(`${API_BASE}/simulate`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(requestBody)
        })
        if (!response.ok) throw new Error('Server error: ' + response.status)
        const data = await response.json()
        console.log('Received from backend:', data)
        renderResults(data)
    } catch (error) {
        alert('Failed to connect to backend. Make sure your server is running.\n\n' + error.message)
    }
}

function renderResults(data) {
    document.getElementById('results').style.display            = 'block'
    document.getElementById('comparison-results').style.display = 'none'
    renderGanttChart(data.ganttChart)
    renderProcessTable(data.processTable)
    renderAverages(data)
}

// ─── Compare mode ─────────────────────────────────────────────────────────────

async function runComparison() {
    const checkboxes  = document.querySelectorAll('#compare-controls input[type=checkbox]:checked')
    const algorithms  = Array.from(checkboxes).map(cb => cb.value)

    if (algorithms.length === 0) {
        alert('Please select at least one algorithm to compare.')
        return
    }

    const rrChecked = algorithms.includes('RR')
    const quantum   = rrChecked ? Number(document.getElementById('quantum').value) : null

    const requestBody = { algorithms, quantum, processes }
    console.log('Sending comparison request:', requestBody)

    try {
        const response = await fetch(`${API_BASE}/compare`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(requestBody)
        })
        if (!response.ok) throw new Error('Server error: ' + response.status)
        const data = await response.json()
        console.log('Received comparison from backend:', data)
        renderComparison(data.results)
    } catch (error) {
        alert('Failed to connect to backend. Make sure your server is running.\n\n' + error.message)
    }
}

function renderComparison(results) {
    document.getElementById('results').style.display            = 'none'
    document.getElementById('comparison-results').style.display = 'block'

    renderComparisonSummary(results)
    renderComparisonCharts(results)
}

/**
 * Renders the summary table with one row per algorithm.
 * The best value in each column gets a green "winner" highlight.
 */
function renderComparisonSummary(results) {
    const tbody = document.getElementById('comparison-summary-body')
    tbody.innerHTML = ''

    const names = Object.keys(results)

    // find the best (lowest/highest) value in each metric column
    const bestWait       = Math.min(...names.map(n => results[n].avgWaitingTime))
    const bestTurnaround = Math.min(...names.map(n => results[n].avgTurnaroundTime))
    const bestThroughput = Math.max(...names.map(n => results[n].throughput))

    names.forEach(name => {
        const r   = results[name]
        const row = document.createElement('tr')

        const isWaitWinner       = r.avgWaitingTime    === bestWait
        const isTurnaroundWinner = r.avgTurnaroundTime === bestTurnaround
        const isThroughputWinner = r.throughput        === bestThroughput

        row.innerHTML = `
            <td><strong>${name}</strong></td>
            <td class="${isWaitWinner       ? 'cell-winner' : ''}">${r.avgWaitingTime.toFixed(2)}   ${isWaitWinner       ? '🏆' : ''}</td>
            <td class="${isTurnaroundWinner ? 'cell-winner' : ''}">${r.avgTurnaroundTime.toFixed(2)} ${isTurnaroundWinner ? '🏆' : ''}</td>
            <td class="${isThroughputWinner ? 'cell-winner' : ''}">${r.throughput.toFixed(4)}         ${isThroughputWinner ? '🏆' : ''}</td>
        `
        tbody.appendChild(row)
    })
}

/**
 * Renders one collapsible algorithm section (Gantt chart + process table)
 * for each algorithm in the comparison.
 */
function renderComparisonCharts(results) {
    const container = document.getElementById('comparison-charts')
    container.innerHTML = ''

    Object.entries(results).forEach(([name, data]) => {
        const section = document.createElement('div')
        section.className = 'compare-section'

        // clickable header to expand/collapse
        const header = document.createElement('div')
        header.className = 'compare-section-header'
        header.innerHTML = `<span>${name}</span><span class="compare-chevron">▼</span>`
        header.onclick = () => {
            body.classList.toggle('collapsed')
            header.querySelector('.compare-chevron').textContent =
                body.classList.contains('collapsed') ? '▶' : '▼'
        }

        const body = document.createElement('div')
        body.className = 'compare-section-body'

        // Gantt chart
        const ganttTitle = document.createElement('h4')
        ganttTitle.textContent = 'Gantt Chart'
        const ganttDiv = document.createElement('div')
        ganttDiv.className = 'gantt-chart'
        buildGanttChart(ganttDiv, data.ganttChart)

        // Process table
        const tableTitle = document.createElement('h4')
        tableTitle.textContent = 'Process Table'
        const table = buildProcessTable(data.processTable)

        body.appendChild(ganttTitle)
        body.appendChild(ganttDiv)
        body.appendChild(tableTitle)
        body.appendChild(table)

        section.appendChild(header)
        section.appendChild(body)
        container.appendChild(section)
    })
}

// ─── Shared rendering helpers ─────────────────────────────────────────────────

/**
 * Populates a gantt container div. Used by both single and compare modes.
 */
function buildGanttChart(container, ganttChart) {
    container.innerHTML = ''
    if (!ganttChart || ganttChart.length === 0) return

    const totalTime = ganttChart[ganttChart.length - 1].end
    const MIN_WIDTH = 800
    const MIN_SCALE = 48
    const SCALE     = Math.max(MIN_SCALE, Math.floor(MIN_WIDTH / totalTime))

    ganttChart.forEach((block, index) => {
        // idle gap
        if (index > 0) {
            const prevBlock = ganttChart[index - 1]
            const gap = block.start - prevBlock.end
            if (gap > 0) {
                const idleDiv = document.createElement('div')
                idleDiv.className = 'gantt-block'
                idleDiv.style.minWidth = (gap * SCALE) + 'px'
                idleDiv.innerHTML = `
                    <div class="gantt-bar gantt-idle">Idle</div>
                    <span class="gantt-time">${prevBlock.end}</span>
                `
                container.appendChild(idleDiv)
            }
        }

        const div      = document.createElement('div')
        div.className  = 'gantt-block'
        const duration = block.end - block.start
        div.style.minWidth = (duration * SCALE) + 'px'
        const colorIdx = getColorIndex(block.processId)
        const waitTime = Math.max(0, block.start - (processes.find(p => p.processId === block.processId)?.arrivalTime ?? block.start))
        div.innerHTML = `
            <div class="gantt-bar gc-${colorIdx}">${block.processId}</div>
            <span class="gantt-time">${block.start}</span>
        `

        const bar     = div.querySelector('.gantt-bar')
        const tooltip = document.createElement('div')
        tooltip.className = 'gantt-tooltip'
        tooltip.innerHTML = `
            <span class="tt-pid">${block.processId}</span>
            <div class="tt-row"><span>Ran</span><span>t=${block.start} → t=${block.end}</span></div>
            <div class="tt-row"><span>Duration</span><span>${duration} unit${duration !== 1 ? 's' : ''}</span></div>
            <div class="tt-row"><span>Waited</span><span>${waitTime} unit${waitTime !== 1 ? 's' : ''}</span></div>
        `
        document.body.appendChild(tooltip)

        bar.addEventListener('mouseenter', () => {
            tooltip.style.display = 'block'
            const rect     = bar.getBoundingClientRect()
            const tipWidth = tooltip.offsetWidth
            const tipHeight= tooltip.offsetHeight
            let left = rect.left + rect.width / 2 - tipWidth / 2
            let top  = rect.top  - tipHeight - 10
            left = Math.max(8, Math.min(left, window.innerWidth - tipWidth - 8))
            tooltip.style.left = left + 'px'
            tooltip.style.top  = top  + 'px'
        })
        bar.addEventListener('mouseleave', () => { tooltip.style.display = 'none' })

        if (index === ganttChart.length - 1) {
            const endLabel = document.createElement('span')
            endLabel.className = 'gantt-time'
            endLabel.style.alignSelf = 'flex-end'
            endLabel.textContent = block.end
            container.appendChild(div)
            container.appendChild(endLabel)
            return
        }
        container.appendChild(div)
    })
}

/**
 * Builds and returns a <table> element for a process table.
 * Used by both single and compare modes.
 */
function buildProcessTable(processTable) {
    const table = document.createElement('table')
    table.className = 'compare-process-table'
    table.innerHTML = `
        <thead>
            <tr>
                <th>Process</th>
                <th>Arrival Time</th>
                <th>Burst Time</th>
                <th>Priority</th>
                <th>Waiting Time</th>
                <th>Turnaround Time</th>
            </tr>
        </thead>
    `
    const tbody = document.createElement('tbody')
    processTable.forEach(p => {
        const colorIdx = getColorIndex(p.processId)
        const color    = getColor(p.processId)
        const row      = document.createElement('tr')
        row.className  = `tr-${colorIdx}`
        row.innerHTML  = `
            <td>
                <div class="td-pid">
                    <div class="td-dot" style="background:${color}"></div>
                    <span class="td-num">${p.processId}</span>
                </div>
            </td>
            <td>${p.arrivalTime}</td>
            <td>${p.burstTime}</td>
            <td>${p.priority}</td>
            <td>${p.waitingTime}</td>
            <td>${p.turnaroundTime}</td>
        `
        tbody.appendChild(row)
    })
    table.appendChild(tbody)
    return table
}

// ─── Single-mode rendering (unchanged API, now delegates to helpers) ──────────

function renderGanttChart(ganttChart) {
    buildGanttChart(document.getElementById('gantt-chart'), ganttChart)
}

function renderProcessTable(processTable) {
    const tbody = document.getElementById('process-table-body')
    tbody.innerHTML = ''
    processTable.forEach(p => {
        const colorIdx = getColorIndex(p.processId)
        const color    = getColor(p.processId)
        const row      = document.createElement('tr')
        row.className  = `tr-${colorIdx}`
        row.innerHTML  = `
            <td>
                <div class="td-pid">
                    <div class="td-dot" style="background:${color}"></div>
                    <span class="td-num">${p.processId}</span>
                </div>
            </td>
            <td>${p.arrivalTime}</td>
            <td>${p.burstTime}</td>
            <td>${p.priority}</td>
            <td>${p.waitingTime}</td>
            <td>${p.turnaroundTime}</td>
        `
        tbody.appendChild(row)
    })
}

function renderAverages(data) {
    const avgWait       = data.avgWaitingTime
    const avgTurnaround = data.avgTurnaroundTime
    const throughput    = data.throughput
    document.getElementById('averages').innerHTML = `
        <div class="avg-card ${waitTier(avgWait)}">
            <div class="avg-label">Avg Waiting Time</div>
            <div class="avg-value">${avgWait.toFixed(2)}</div>
            <span class="avg-hint">${waitHint(avgWait)}</span>
        </div>
        <div class="avg-card ${turnaroundTier(avgTurnaround)}">
            <div class="avg-label">Avg Turnaround Time</div>
            <div class="avg-value">${avgTurnaround.toFixed(2)}</div>
            <span class="avg-hint">${turnaroundHint(avgTurnaround)}</span>
        </div>
        <div class="avg-card ${throughputTier(throughput)}">
            <div class="avg-label">Throughput</div>
            <div class="avg-value">${throughput.toFixed(4)}</div>
            <span class="avg-hint">${throughputHint(throughput)}</span>
        </div>
    `
}

function waitTier(v)       { return v <= 4   ? 'stat-green' : v <= 10  ? 'stat-amber' : 'stat-red' }
function turnaroundTier(v) { return v <= 8   ? 'stat-green' : v <= 16  ? 'stat-amber' : 'stat-red' }
function throughputTier(v) { return v >= 0.3 ? 'stat-green' : v >= 0.15? 'stat-amber' : 'stat-red' }
function waitHint(v)       { return v <= 4   ? 'Low — great' : v <= 10  ? 'Moderate' : 'High — consider SJF/SRTF' }
function turnaroundHint(v) { return v <= 8   ? 'Low — great' : v <= 16  ? 'Moderate' : 'High — processes waited long' }
function throughputHint(v) { return v >= 0.3 ? 'High — efficient' : v >= 0.15 ? 'Moderate' : 'Low — CPU underutilized' }














