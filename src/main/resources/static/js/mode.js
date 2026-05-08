// Mode toggle (Single / Compare)

export let currentMode = 'single'

export function setMode(mode) {
    currentMode = mode

    document.getElementById('single-controls').style.display  =
        mode === 'single' ? 'flex' : 'none'
    document.getElementById('compare-controls').style.display =
        mode === 'compare' ? 'flex' : 'none'

    document.getElementById('btn-single').classList.toggle('mode-btn-active',  mode === 'single')
    document.getElementById('btn-compare').classList.toggle('mode-btn-active', mode === 'compare')

    // hide both result panels when switching modes
    document.getElementById('results').style.display            = 'none'
    document.getElementById('comparison-results').style.display = 'none'

    updateQuantumVisibility()
}

export function updateQuantumVisibility() {
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

export function handleAlgorithmChange() {
    updateQuantumVisibility()
}
