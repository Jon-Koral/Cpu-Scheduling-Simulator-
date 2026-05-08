// Shared application state

export const processes = []
export let processCounter = 1
export function incrementCounter() { processCounter++ }

export const PROCESS_COLORS = [
    'indianred',
    'peru',
    'goldenrod',
    'mediumseagreen',
    'steelblue',
    'slateblue',
    'mediumvioletred',
    'lightseagreen',
]

export const processColorMap = {}

export function getColorIndex(processId) {
    return processColorMap[processId] || 1
}

export function getColor(processId) {
    return PROCESS_COLORS[(getColorIndex(processId) - 1) % PROCESS_COLORS.length]
}

export function resetProcesses() {
    processes.length = 0
    processCounter = 1
    Object.keys(processColorMap).forEach(k => delete processColorMap[k])
}

export function setProcessCounter(n) {
    processCounter = n
}