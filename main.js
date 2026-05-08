const processes = []

let processCounter = 1

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

function openModal() {
    document.getElementById('modal').classList.add('active')
    document.getElementById('arrival-time').focus()
    document.getElementById('modal-error').textContent = ''
}

function closeModal() {
    document.getElementById('modal').classList.remove('active')
    // clear all the inputs
    document.getElementById('arrival-time').value = ''
    document.getElementById('burst-time').value = ''
    document.getElementById('priority').value = ''
    document.getElementById('modal-error').textContent = ''
}

function handleOverlayClick(e) {
    if(e.target === document.getElementById('modal')) {
        closeModal()
    }
}

function addProcess() {
    // get the arrival and burst time
    const arrivalTime = document.getElementById('arrival-time').value
    const burstTime = document.getElementById('burst-time').value
    const priority = document.getElementById('priority').value

    // check if fields are empty
    if(arrivalTime === '' || burstTime === '') {
        document.getElementById('modal-error').textContent = 'Arrival Time or Burst Time is required.'
        return;
    }

    const processId = 'P' + processCounter
    const colorIndex = ((processCounter - 1) % PROCESS_COLORS.length) + 1
    processColorMap[processId] = colorIndex

    // build the process
    const process = {
        processId: processId,
        arrivalTime: Number(arrivalTime),
        burstTime: Number(burstTime),
        priority: priority !== '' ? Number(priority) : 0
    }

    // put the new process in the process array
    processes.push(process)
    processCounter++

    // render the process as a card in the list
    renderProcessCard(process, colorIndex)

    // close the modal
    closeModal()
}

function renderProcessCard(process, colorIndex) {
    const idx = colorIndex || getColorIndex(process.processId)
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
    // remove the process from the array
    const index = processes.findIndex(p => p.processId === processId)
    if (index !== -1) {
        processes.splice(index, 1)
    }

    // remove the card from the screen
    const card = document.getElementById('card-' + processId)
    if (card) {
        card.remove()
    }
}

// shows the quantum input only when RR is selected
function handleAlgorithmChange() {
    const algorithm = document.getElementById('algorithm').value
    const quantumGroup = document.getElementById('quantum-group')

    if(algorithm === 'RR') {
        quantumGroup.classList.add('visible')
    } else {
        quantumGroup.classList.remove('visible')
    }
}

async function runAlgorithm() {
    if(processes.length === 0) {
        alert('Please add at least one process first.')
        return
    }

    const algorithm = document.getElementById('algorithm').value
    const quantum = algorithm === 'RR' ? Number(document.getElementById('quantum').value) : null

    // build the request body the backend needs
    const requestBody = {
        algorithm: algorithm,
        quantum: quantum,
        processes: processes
    }

    console.log('Sending JSON to the backend:', requestBody)

    try {
        const response = await fetch('https://cpu-scheduling-simulator-4.onrender.com/api/simulate', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(requestBody)
        })

        if (!response.ok) {
            throw new Error('Server error: ' + response.status)
        }

        const data = await response.json()
        console.log('Received from backend:', data)

        renderResults(data)

    } catch (error) {
        alert('Failed to connect to backend. Make sure your server is running.\n\n' + error.message)
    }
}

function renderResults(data) {
    // show the results section
    document.getElementById('results').style.display = 'block'

    renderGanttChart(data.ganttChart)
    renderProcessTable(data.processTable)
    renderAverages(data)
}

function renderGanttChart(ganttChart) {
    const container = document.getElementById('gantt-chart')
    container.innerHTML = ''

    // compute total time span so we can fill the container
    const totalTime = ganttChart[ganttChart.length - 1].end
    const MIN_WIDTH = 800
    const MIN_SCALE = 48
    const SCALE = Math.max(MIN_SCALE, Math.floor(MIN_WIDTH / totalTime))

    ganttChart.forEach((block, index) => {

        // check if there's an idle gap before this block
        if (index > 0) {
            const prevBlock = ganttChart[index - 1]
            const gap = block.start - prevBlock.end

            if (gap > 0) {
                // put an idle block to represent idle time
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

        // render the actual process block
        const div = document.createElement('div')
        div.className = 'gantt-block'
        const duration = block.end - block.start
        div.style.minWidth = (duration * SCALE) + 'px'
        const colorIdx = getColorIndex(block.processId)
        const waitTime = Math.max(0, block.start - (processes.find(p => p.processId === block.processId)?.arrivalTime ?? block.start))
        div.innerHTML = `
            <div class="gantt-bar gc-${colorIdx}">${block.processId}</div>
            <span class="gantt-time">${block.start}</span>
        `

        const bar = div.querySelector('.gantt-bar')
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
           const rect = bar.getBoundingClientRect()
           const tipWidth = tooltip.offsetWidth
           const tipHeight = tooltip.offsetHeight
           let left = rect.left + rect.width / 2 - tipWidth / 2
           let top = rect.top - tipHeight - 10
           left = Math.max(8, Math.min(left, window.innerWidth - tipWidth - 8))
           tooltip.style.left = left + 'px'
           tooltip.style.top = top + 'px'
       })

        bar.addEventListener('mouseleave', () => {
            tooltip.style.display = 'none'
        })

        // add the final end time label after the last block
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

function renderProcessTable(processTable) {
    const tbody = document.getElementById('process-table-body')
    tbody.innerHTML = ''

    processTable.forEach(p => {
        const colorIdx = getColorIndex(p.processId)
        const color    = getColor(p.processId)
        const row = document.createElement('tr')
        row.className = `tr-${colorIdx}`
        row.innerHTML = `
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
    const avgWait = data.avgWaitingTime
    const avgTurnaround = data.avgTurnaroundTime
    const throughput = data.throughput
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
function waitTier(v) {
    return v <= 4  ? 'stat-green' : v <= 10 ? 'stat-amber' : 'stat-red'
}
function turnaroundTier(v) {
    return v <= 8  ? 'stat-green' : v <= 16 ? 'stat-amber' : 'stat-red'
}
function throughputTier(v) {
    return v >= 0.3 ? 'stat-green' : v >= 0.15 ? 'stat-amber' : 'stat-red'
}
function waitHint(v) {
    return v <= 4  ? 'Low — great' : v <= 10 ? 'Moderate' : 'High — consider SJF/SRTF'
}
function turnaroundHint(v) {
    return v <= 8  ? 'Low — great' : v <= 16 ? 'Moderate' : 'High — processes waited long'
}
function throughputHint(v) {
    return v >= 0.3 ? 'High — efficient' : v >= 0.15 ? 'Moderate' : 'Low — CPU underutilized'
}














