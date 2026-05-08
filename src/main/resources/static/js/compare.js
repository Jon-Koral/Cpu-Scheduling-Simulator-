import { buildGanttChart, buildProcessRow, waitTier, turnaroundTier, throughputTier } from './render.js'

// Compare-mode results

export function renderComparison(results) {
    document.getElementById('results').style.display            = 'none'
    document.getElementById('comparison-results').style.display = 'block'

    renderComparisonSummary(results)
    renderComparisonCharts(results)
}

// Summary table
// One row per algorithm. The best value in each metric column gets a winner highlight.

function renderComparisonSummary(results) {
    const tbody = document.getElementById('comparison-summary-body')
    tbody.innerHTML = ''

    const names = Object.keys(results)

    const bestWait       = Math.min(...names.map(n => results[n].avgWaitingTime))
    const bestTurnaround = Math.min(...names.map(n => results[n].avgTurnaroundTime))
    const bestThroughput = Math.max(...names.map(n => results[n].throughput))

    names.forEach(name => {
        const r = results[name]

        const isWaitWinner       = r.avgWaitingTime    === bestWait
        const isTurnaroundWinner = r.avgTurnaroundTime === bestTurnaround
        const isThroughputWinner = r.throughput        === bestThroughput

        const row = document.createElement('tr')
        row.innerHTML = `
            <td><strong>${name}</strong></td>
            <td class="${isWaitWinner       ? 'cell-winner' : ''}">${r.avgWaitingTime.toFixed(2)}    ${isWaitWinner       ? '🏆' : ''}</td>
            <td class="${isTurnaroundWinner ? 'cell-winner' : ''}">${r.avgTurnaroundTime.toFixed(2)} ${isTurnaroundWinner ? '🏆' : ''}</td>
            <td class="${isThroughputWinner ? 'cell-winner' : ''}">${r.throughput.toFixed(4)}         ${isThroughputWinner ? '🏆' : ''}</td>
        `
        tbody.appendChild(row)
    })
}

// Per-algorithm collapsible sections
// One expandable card per algorithm, each containing a Gantt chart and process table.

function renderComparisonCharts(results) {
    const container = document.getElementById('comparison-charts')
    container.innerHTML = ''

    Object.entries(results).forEach(([name, data]) => {
        const section = document.createElement('div')
        section.className = 'compare-section'

        // clickable header toggles the body open/closed
        const header = document.createElement('div')
        header.className = 'compare-section-header'
        header.innerHTML = `<span>${name}</span><span class="compare-chevron">▼</span>`

        const body = document.createElement('div')
        body.className = 'compare-section-body'

        header.addEventListener('click', () => {
            body.classList.toggle('collapsed')
            header.querySelector('.compare-chevron').textContent =
                body.classList.contains('collapsed') ? '▶' : '▼'
        })

        // Gantt chart
        const ganttTitle = document.createElement('h4')
        ganttTitle.textContent = 'Gantt Chart'
        const ganttDiv = document.createElement('div')
        ganttDiv.className = 'gantt-chart'
        buildGanttChart(ganttDiv, data.ganttChart)

        // Process table
        const tableTitle = document.createElement('h4')
        tableTitle.textContent = 'Process Table'
        const table = buildCompareProcessTable(data.processTable)

        body.appendChild(ganttTitle)
        body.appendChild(ganttDiv)
        body.appendChild(tableTitle)
        body.appendChild(table)

        section.appendChild(header)
        section.appendChild(body)
        container.appendChild(section)
    })
}

// Builds a full <table> element for the compare mode process tables.
function buildCompareProcessTable(processTable) {
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
    processTable.forEach(p => tbody.appendChild(buildProcessRow(p)))
    table.appendChild(tbody)
    return table
}
