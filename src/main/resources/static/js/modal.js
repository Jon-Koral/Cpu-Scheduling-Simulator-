import {
    processes,
    processCounter,
    incrementCounter,
    PROCESS_COLORS,
    processColorMap,
} from './state.js'

// Modal open / close

export function openModal() {
    document.getElementById('modal').classList.add('active')
    document.getElementById('arrival-time').focus()
    document.getElementById('modal-error').textContent = ''
}

export function closeModal() {
    document.getElementById('modal').classList.remove('active')
    document.getElementById('arrival-time').value      = ''
    document.getElementById('burst-time').value        = ''
    document.getElementById('priority').value          = ''
    document.getElementById('modal-error').textContent = ''
}

export function handleOverlayClick(e) {
    if (e.target === document.getElementById('modal')) closeModal()
}

// Add / delete

export function addProcess() {
    const arrivalTime = document.getElementById('arrival-time').value
    const burstTime   = document.getElementById('burst-time').value
    const priority    = document.getElementById('priority').value

    if (arrivalTime === '' || burstTime === '') {
        document.getElementById('modal-error').textContent =
            'Arrival Time or Burst Time is required.'
        return
    }

    const processId  = 'P' + processCounter
    const colorIndex = ((processCounter - 1) % PROCESS_COLORS.length) + 1
    processColorMap[processId] = colorIndex

    const process = {
        processId,
        arrivalTime: Number(arrivalTime),
        burstTime:   Number(burstTime),
        priority:    priority !== '' ? Number(priority) : 0,
    }

    processes.push(process)
    incrementCounter()
    renderProcessCard(process, colorIndex)
    closeModal()
}

export function deleteProcess(processId) {
    const index = processes.findIndex(p => p.processId === processId)
    if (index !== -1) processes.splice(index, 1)
    const card = document.getElementById('card-' + processId)
    if (card) card.remove()
}

// Process card rendering

export function renderProcessCard(process, colorIndex) {
    const idx  = colorIndex || 1
    const card = document.createElement('div')
    card.className = `process-card pc-${idx}`
    card.id        = 'card-' + process.processId
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
        <button class="btn-delete" data-id="${process.processId}">Delete</button>
    `
    // attach delete listener so we don't need a global function
    card.querySelector('.btn-delete').addEventListener('click', () =>
        deleteProcess(process.processId)
    )
    document.getElementById('process-list').appendChild(card)
}

// Random process generation

export function generateRandomProcesses() {
    const count = parseInt(document.getElementById('random-count').value) || 5

    if (count < 1 || count > 20) {
        alert('Please enter a count between 1 and 20.')
        return
    }

    for (let i = 0; i < count; i++) {
        const processId  = 'P' + processCounter
        const colorIndex = ((processCounter - 1) % PROCESS_COLORS.length) + 1
        processColorMap[processId] = colorIndex

        const process = {
            processId,
            arrivalTime: Math.floor(Math.random() * 15),
            burstTime:   Math.floor(Math.random() * 10) + 1,
            priority:    Math.floor(Math.random() * 10) + 1,
        }

        processes.push(process)
        incrementCounter()
        renderProcessCard(process, colorIndex)
    }
}