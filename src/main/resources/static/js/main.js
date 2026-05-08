import { openModal, closeModal, handleOverlayClick, addProcess, generateRandomProcesses } from './modal.js'
import { setMode, handleAlgorithmChange, updateQuantumVisibility } from './mode.js'
import { initRecents } from './recents.js'
import { initTheme } from './theme.js'
import { run } from './api.js'

// Boot
// All event listeners are attached here once the DOM is ready.
// No logic lives in this file — it just wires everything together.

document.addEventListener('DOMContentLoaded', () => {
    initRecents(run)
    initTheme()

    // mode toggle
    document.getElementById('btn-single').addEventListener('click',  () => setMode('single'))
    document.getElementById('btn-compare').addEventListener('click', () => setMode('compare'))

    // algorithm dropdown (single mode)
    document.getElementById('algorithm').addEventListener('change', handleAlgorithmChange)

    // compare checkboxes — show/hide quantum when RR is ticked
    document.querySelectorAll('#compare-controls input[type=checkbox]').forEach(cb => {
        cb.addEventListener('change', updateQuantumVisibility)
    })

    // modal
    document.getElementById('btn-open-modal').addEventListener('click', openModal)
    document.getElementById('btn-add-process').addEventListener('click', addProcess)
    document.getElementById('btn-cancel').addEventListener('click', closeModal)
    document.getElementById('modal').addEventListener('click', handleOverlayClick)
    document.getElementById('btn-generate-random').addEventListener('click', generateRandomProcesses)

    // run button
    document.getElementById('run-algorithm').addEventListener('click', run)

    // start in single mode
    setMode('single')
})
