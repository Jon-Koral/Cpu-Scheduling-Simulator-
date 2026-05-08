import { processes }  from './state.js'
import { currentMode } from './mode.js'
import { renderResults }     from './render.js'
import { renderComparison }  from './compare.js'
import { saveRecent } from './recents.js'

// Switch this one line to point at your local backend while developing,
// or leave it as-is for production.
const API_BASE = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : 'https://cpu-scheduling-simulator-4.onrender.com/api'

// Entry point called by the Run button

export async function run() {
    if (processes.length === 0) {
        alert('Please add at least one process first.')
        return;
    }
    if (currentMode === 'single') {
        await runAlgorithm()
    } else {
        await runComparison()
    }
}

// Single-algorithm simulation

async function runAlgorithm() {
    const algorithm = document.getElementById('algorithm').value
    const quantum   = algorithm === 'RR'
        ? Number(document.getElementById('quantum').value)
        : null

    const requestBody = { algorithm, quantum, processes }
    console.log('Sending to /simulate:', requestBody)

    try {
        const response = await fetch(`${API_BASE}/simulate`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(requestBody),
        })
        if (!response.ok) throw new Error('Server error: ' + response.status)
        const data = await response.json()
        console.log('Received from /simulate:', data)
        renderResults(data)
        saveRecent({
            mode:              'single',
            algorithm,
            quantum:           quantum || null,
            processCount:      processes.length,
            processes:         processes.map(p => ({ ...p })),
            avgWaitingTime:    data.avgWaitingTime,
            avgTurnaroundTime: data.avgTurnaroundTime,
            throughput:        data.throughput,
        })
    } catch (error) {
        alert('Failed to connect to backend. Make sure your server is running.\n\n' + error.message)
    }
}

// Multi-algorithm comparison

async function runComparison() {
    const checkboxes = document.querySelectorAll('#compare-controls input[type=checkbox]:checked')
    const algorithms = Array.from(checkboxes).map(cb => cb.value)

    if (algorithms.length === 0) {
        alert('Please select at least one algorithm to compare.')
        return
    }

    const rrChecked = algorithms.includes('RR')
    const quantum   = rrChecked
        ? Number(document.getElementById('quantum').value)
        : null

    const requestBody = { algorithms, quantum, processes }
    console.log('Sending to /compare:', requestBody)

    try {
        const response = await fetch(`${API_BASE}/compare`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(requestBody),
        })
        if (!response.ok) throw new Error('Server error: ' + response.status)
        const data = await response.json()
        console.log('Received from /compare:', data)
        renderComparison(data.results)
        saveRecent({
            mode:         'compare',
            algorithms,
            quantum:      quantum || null,
            processCount: processes.length,
            processes:    processes.map(p => ({ ...p })),
        })
    } catch (error) {
        alert('Failed to connect to backend. Make sure your server is running.\n\n' + error.message)
    }
}
