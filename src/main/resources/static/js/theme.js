const STORAGE_KEY = 'cpu-sim-theme'

export function initTheme() {
    const saved = localStorage.getItem(STORAGE_KEY) || 'light'
    applyTheme(saved)

    document.querySelectorAll('.theme-btn').forEach(btn => {
        btn.addEventListener('click', () => applyTheme(btn.dataset.theme))
    })
}

function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(STORAGE_KEY, theme)
    document.querySelectorAll('.theme-btn').forEach(btn => {
        btn.classList.toggle('theme-btn-active', btn.dataset.theme === theme)
    })
}