/* ============================================================
   SecureEdu — Shared UI Utilities
   Theme toggle (dark/light) + helpers
   ============================================================ */

(function () {
    'use strict';

    // ============================================================
    // THEME TOGGLE
    // ============================================================
    const STORAGE_KEY = 'secureedu_theme';

    function isLight() { return document.body.classList.contains('light-mode'); }

    function applyTheme(light) {
        if (light) {
            document.body.classList.add('light-mode');
        } else {
            document.body.classList.remove('light-mode');
        }
        updateToggleUI();
        localStorage.setItem(STORAGE_KEY, light ? 'light' : 'dark');
    }

    function updateToggleUI() {
        const icon  = document.getElementById('themeIcon');
        const label = document.getElementById('themeLabel');
        if (!icon) return;
        if (isLight()) {
            icon.textContent  = '☀️';
            label.textContent = 'Light';
        } else {
            icon.textContent  = '🌙';
            label.textContent = 'Dark';
        }
    }

    function initTheme() {
        const saved = localStorage.getItem(STORAGE_KEY);
        applyTheme(saved === 'light');
    }

    window.toggleTheme = function () {
        applyTheme(!isLight());
    };

    // Init on DOM ready
    document.addEventListener('DOMContentLoaded', initTheme);

    // ============================================================
    // INJECT TOGGLE BUTTON (call once in each page)
    // ============================================================
    window.injectThemeToggle = function () {
        const btn = document.createElement('button');
        btn.className = 'theme-toggle-btn';
        btn.id = 'themeToggleBtn';
        btn.onclick = window.toggleTheme;
        btn.title = 'Switch theme';
        btn.setAttribute('aria-label', 'Toggle dark/light mode');
        btn.innerHTML = `
            <span class="theme-toggle-icon" id="themeIcon">🌙</span>
            <div class="toggle-pill">
                <div class="toggle-thumb"></div>
            </div>
            <span class="toggle-label" id="themeLabel">Dark</span>
        `;
        document.body.appendChild(btn);
        updateToggleUI();
    };

    document.addEventListener('DOMContentLoaded', function () {
        window.injectThemeToggle();
    });

})();
