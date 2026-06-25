/**
 * ShopNest Client Side Interactions
 */
document.addEventListener('DOMContentLoaded', function () {
    console.log("ShopNest Script System Loaded.");

    // 1. Auto-dismiss flash alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            // Check if bootstrap is defined to close it using the API
            if (typeof bootstrap !== 'undefined' && bootstrap.Alert) {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            } else {
                // Fallback direct DOM removal with fade transition
                alert.style.transition = 'opacity 0.5s ease';
                alert.style.opacity = '0';
                setTimeout(function () {
                    alert.remove();
                }, 500);
            }
        }, 5000);
    });

    // 2. Interactive Scroll Effects for Navigation Bar
    const headerNav = document.querySelector('.header-nav');
    if (headerNav) {
        window.addEventListener('scroll', function () {
            if (window.scrollY > 20) {
                headerNav.style.padding = '8px 0';
                headerNav.style.boxShadow = '0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -4px rgba(0, 0, 0, 0.05)';
                headerNav.style.background = 'rgba(255, 255, 255, 0.95)';
            } else {
                headerNav.style.padding = '16px 0';
                headerNav.style.boxShadow = 'none';
                headerNav.style.background = 'rgba(255, 255, 255, 0.85)';
            }
        });
    }

    // 3. Client Side Input Validation helpers
    const qtyInputs = document.querySelectorAll('.qty-input');
    qtyInputs.forEach(function(input) {
        input.addEventListener('keypress', function(e) {
            // Prevent entering characters that aren't digits
            if (e.key === '-' || e.key === '+' || e.key === 'e' || e.key === '.') {
                e.preventDefault();
            }
        });
    });
});
