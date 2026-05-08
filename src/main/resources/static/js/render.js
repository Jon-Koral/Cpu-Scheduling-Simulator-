import { processes, getColorIndex, getColor } from './state.js'

// Single-mode results

export function renderResults(data) {
    document.getElementById('results').style.display            = 'block'
    document.getElementById('comparison-results').style.display = 'none'
    buildGanttChart(document.getElementById('gantt-chart'), data.ganttChart)
    renderProcessTable(data.processTable)
    renderAverages(data)
}

function renderProcessTable(processTable) {
    const tbody = document.getElementById('process-table-body')
    tbody.innerHTML = ''
    processTable.forEach(p => {
        tbody.appendChild(buildProcessRow(p))
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

// Shared Gantt chart builder
// Used by both single-mode (render.js) and compare-mode (compare.js).

export function buildGanttChart(container, ganttChart) {
    container.innerHTML = ''
    if (!ganttChart || ganttChart.length === 0) return

    const totalTime = ganttChart[ganttChart.length - 1].end
    const MIN_SCALE = 48
    const SCALE     = Math.max(MIN_SCALE, Math.floor(800 / totalTime))

    ganttChart.forEach((block, index) => {
        // idle gap between the previous block and this one
        if (index > 0) {
            const gap = block.start - ganttChart[index - 1].end
            if (gap > 0) {
                const idleDiv = document.createElement('div')
                idleDiv.className = 'gantt-block'
                idleDiv.style.minWidth = (gap * SCALE) + 'px'
                idleDiv.innerHTML = `
                    <div class="gantt-bar gantt-idle">Idle</div>
                    <span class="gantt-time">${ganttChart[index - 1].end}</span>
                `
                container.appendChild(idleDiv)
            }
        }

        const duration = block.end - block.start
        const colorIdx = getColorIndex(block.processId)
        const waitTime = Math.max(
            0,
            block.start - (processes.find(p => p.processId === block.processId)?.arrivalTime ?? block.start)
        )

        const div = document.createElement('div')
        div.className      = 'gantt-block'
        div.style.minWidth = (duration * SCALE) + 'px'
        div.innerHTML = `
            <div class="gantt-bar gc-${colorIdx}">${block.processId}</div>
            <span class="gantt-time">${block.start}</span>
        `

        // tooltip
        const tooltip = document.createElement('div')
        tooltip.className = 'gantt-tooltip'
        tooltip.innerHTML = `
            <span class="tt-pid">${block.processId}</span>
            <div class="tt-row"><span>Ran</span><span>t=${block.start} → t=${block.end}</span></div>
            <div class="tt-row"><span>Duration</span><span>${duration} unit${duration !== 1 ? 's' : ''}</span></div>
            <div class="tt-row"><span>Waited</span><span>${waitTime} unit${waitTime !== 1 ? 's' : ''}</span></div>
        `
        document.body.appendChild(tooltip)

        const bar = div.querySelector('.gantt-bar')
        bar.addEventListener('mouseenter', () => {
            tooltip.style.display = 'block'
            const rect = bar.getBoundingClientRect()
            let left = rect.left + rect.width / 2 - tooltip.offsetWidth / 2
            let top  = rect.top  - tooltip.offsetHeight - 10
            left = Math.max(8, Math.min(left, window.innerWidth - tooltip.offsetWidth - 8))
            tooltip.style.left = left + 'px'
            tooltip.style.top  = top  + 'px'
        })
        bar.addEventListener('mouseleave', () => { tooltip.style.display = 'none' })

        // end-time label after the last block
        if (index === ganttChart.length - 1) {
            const endLabel = document.createElement('span')
            endLabel.className   = 'gantt-time'
            endLabel.style.alignSelf = 'flex-end'
            endLabel.textContent = block.end
            container.appendChild(div)
            container.appendChild(endLabel)
            return
        }

        container.appendChild(div)
    })
}

// Shared process table row builder
// Returns a <tr> element. Used by both single-mode and compare-mode.

export function buildProcessRow(p) {
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
    return row
}

// Stat tier helpers

export function waitTier(v)       { return v <= 4   ? 'stat-green' : v <= 10   ? 'stat-amber' : 'stat-red' }
export function turnaroundTier(v) { return v <= 8   ? 'stat-green' : v <= 16   ? 'stat-amber' : 'stat-red' }
export function throughputTier(v) { return v >= 0.3 ? 'stat-green' : v >= 0.15 ? 'stat-amber' : 'stat-red' }

function waitHint(v)       { return v <= 4   ? 'Low — great' : v <= 10   ? 'Moderate' : 'High — consider SJF/SRTF' }
function turnaroundHint(v) { return v <= 8   ? 'Low — great' : v <= 16   ? 'Moderate' : 'High — processes waited long' }
function throughputHint(v) { return v >= 0.3 ? 'High — efficient' : v >= 0.15 ? 'Moderate' : 'Low — CPU underutilized' }
