/* ============================================================
   SecureEdu — Celebration Fireworks Animation
   Triggers on dashboard load for 3 seconds
   ============================================================ */

(function () {
    'use strict';

    // ---- Config by role / page ----
    const ROLE_COLORS = {
        student: ['#06B6D4','#22c55e','#3B82F6','#a78bfa','#fff','#86efac','#67e8f9'],
        staff:   ['#F59E0B','#f97316','#fbbf24','#fb923c','#fff','#fde68a','#fed7aa'],
        admin:   ['#EF4444','#f59e0b','#7C3AED','#06B6D4','#fff','#fca5a5','#fde68a'],
        default: ['#7C3AED','#06B6D4','#10B981','#F59E0B','#EF4444','#3B82F6','#fff']
    };

    function getRole() {
        const body = document.body;
        if (body.classList.contains('student-role')) return 'student';
        if (body.classList.contains('staff-role'))   return 'staff';
        if (body.classList.contains('admin-role'))   return 'admin';
        return 'default';
    }

    // ---- Particle class ----
    function Particle(x, y, colors) {
        this.x = x;
        this.y = y;
        this.color = colors[Math.floor(Math.random() * colors.length)];
        this.radius = Math.random() * 4 + 1;
        this.angle = Math.random() * Math.PI * 2;
        this.speed = Math.random() * 8 + 2;
        this.vx = Math.cos(this.angle) * this.speed;
        this.vy = Math.sin(this.angle) * this.speed - 3;
        this.gravity = 0.18;
        this.life = 1.0;
        this.decay = Math.random() * 0.018 + 0.008;
        this.trail = [];
        this.isStar = Math.random() > 0.5;
        this.rotation = Math.random() * Math.PI * 2;
        this.rotSpeed = (Math.random() - 0.5) * 0.2;
    }

    Particle.prototype.update = function () {
        this.vx *= 0.97;
        this.vy += this.gravity;
        this.x += this.vx;
        this.y += this.vy;
        this.life -= this.decay;
        this.rotation += this.rotSpeed;
        this.trail.push({ x: this.x, y: this.y });
        if (this.trail.length > 5) this.trail.shift();
    };

    Particle.prototype.draw = function (ctx) {
        ctx.save();
        ctx.globalAlpha = Math.max(0, this.life);
        ctx.fillStyle = this.color;
        ctx.strokeStyle = this.color;

        if (this.isStar) {
            // Draw a 4-pointed star
            ctx.translate(this.x, this.y);
            ctx.rotate(this.rotation);
            ctx.beginPath();
            for (let i = 0; i < 4; i++) {
                const angle = (i * Math.PI) / 2;
                const len = this.radius * 2.5;
                ctx.lineTo(Math.cos(angle) * len, Math.sin(angle) * len);
                ctx.lineTo(Math.cos(angle + Math.PI / 4) * this.radius * 0.8,
                           Math.sin(angle + Math.PI / 4) * this.radius * 0.8);
            }
            ctx.closePath();
            ctx.fill();
        } else {
            // Draw streak trail
            if (this.trail.length > 1) {
                ctx.globalAlpha = Math.max(0, this.life * 0.4);
                ctx.lineWidth = this.radius * 0.6;
                ctx.beginPath();
                ctx.moveTo(this.trail[0].x, this.trail[0].y);
                for (let t = 1; t < this.trail.length; t++) {
                    ctx.lineTo(this.trail[t].x, this.trail[t].y);
                }
                ctx.stroke();
                ctx.globalAlpha = Math.max(0, this.life);
            }
            // Circle particle
            ctx.beginPath();
            ctx.arc(0, 0, this.radius, 0, Math.PI * 2);
            ctx.fill();
        }
        ctx.restore();
    };

    // ---- Rocket (rises and explodes) ----
    function Rocket(colors) {
        this.x = Math.random() * window.innerWidth;
        this.y = window.innerHeight + 10;
        this.vy = -(Math.random() * 14 + 10);
        this.targetY = Math.random() * (window.innerHeight * 0.55) + 60;
        this.colors = colors;
        this.exploded = false;
        this.particles = [];
        this.active = true;
    }

    Rocket.prototype.update = function () {
        if (!this.exploded) {
            this.y += this.vy;
            if (this.y <= this.targetY) {
                this.explode();
            }
        } else {
            this.particles = this.particles.filter(p => p.life > 0);
            this.particles.forEach(p => p.update());
            if (this.particles.length === 0) this.active = false;
        }
    };

    Rocket.prototype.explode = function () {
        this.exploded = true;
        const count = 80 + Math.floor(Math.random() * 60);
        for (let i = 0; i < count; i++) {
            this.particles.push(new Particle(this.x, this.y, this.colors));
        }
        // Extra sparkles at explosion center
        for (let i = 0; i < 15; i++) {
            const p = new Particle(this.x, this.y, ['#fff', '#fffde7', '#fff9c4']);
            p.speed = Math.random() * 3 + 0.5;
            p.vx = Math.cos(p.angle) * p.speed;
            p.vy = Math.sin(p.angle) * p.speed;
            p.radius = Math.random() * 2 + 1;
            p.decay = 0.06;
            this.particles.push(p);
        }
    };

    Rocket.prototype.draw = function (ctx) {
        if (!this.exploded) {
            ctx.save();
            ctx.globalAlpha = 0.9;
            ctx.fillStyle = '#fff';
            ctx.shadowColor = this.colors[0];
            ctx.shadowBlur = 12;
            ctx.beginPath();
            ctx.arc(this.x, this.y, 3, 0, Math.PI * 2);
            ctx.fill();
            // Tail
            const grad = ctx.createLinearGradient(this.x, this.y, this.x, this.y + 30);
            grad.addColorStop(0, 'rgba(255,255,255,0.9)');
            grad.addColorStop(1, 'rgba(255,200,100,0)');
            ctx.strokeStyle = grad;
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(this.x, this.y);
            ctx.lineTo(this.x, this.y + 30);
            ctx.stroke();
            ctx.restore();
        } else {
            this.particles.forEach(p => p.draw(ctx));
        }
    };

    // ---- Burst (immediate explosion, no rocket) ----
    function Burst(x, y, colors) {
        this.particles = [];
        this.active = true;
        const count = 50 + Math.floor(Math.random() * 30);
        for (let i = 0; i < count; i++) {
            this.particles.push(new Particle(x, y, colors));
        }
    }

    Burst.prototype.update = function () {
        this.particles = this.particles.filter(p => p.life > 0);
        this.particles.forEach(p => p.update());
        if (this.particles.length === 0) this.active = false;
    };

    Burst.prototype.draw = function (ctx) {
        this.particles.forEach(p => p.draw(ctx));
    };

    // ---- Main fireworks launcher ----
    function Fireworks(colors) {
        this.canvas  = document.createElement('canvas');
        this.ctx     = this.canvas.getContext('2d');
        this.rockets = [];
        this.bursts  = [];
        this.colors  = colors;
        this.frame   = 0;
        this.running = true;
        this.alpha   = 1;

        // Style the overlay canvas
        Object.assign(this.canvas.style, {
            position:  'fixed',
            top: '0',
            left: '0',
            width: '100%',
            height: '100%',
            pointerEvents: 'none',
            zIndex: '99999'
        });

        this.canvas.width  = window.innerWidth;
        this.canvas.height = window.innerHeight;
        document.body.appendChild(this.canvas);

        // Show welcome banner
        this.showBanner();

        // Launch pattern: dense at start
        this.schedule();

        // Start animation
        this.loop = this.loop.bind(this);
        requestAnimationFrame(this.loop);

        // Stop after 3s, fade out
        setTimeout(() => {
            this.fadeOut();
        }, 2800);
    }

    Fireworks.prototype.schedule = function () {
        const times = [0,150,300,450,600,700,800,950,1100,1250,1400,1600,1800,2000,2200,2500];
        times.forEach(t => {
            setTimeout(() => {
                if (this.running) this.launchRocket();
            }, t);
        });
        // Immediate bursts along the edges
        setTimeout(() => { this.burst(this.canvas.width * 0.15, this.canvas.height * 0.25); }, 0);
        setTimeout(() => { this.burst(this.canvas.width * 0.85, this.canvas.height * 0.30); }, 200);
        setTimeout(() => { this.burst(this.canvas.width * 0.5,  this.canvas.height * 0.15); }, 400);
        setTimeout(() => { this.burst(this.canvas.width * 0.25, this.canvas.height * 0.35); }, 800);
        setTimeout(() => { this.burst(this.canvas.width * 0.75, this.canvas.height * 0.20); }, 1200);
        setTimeout(() => { this.burst(this.canvas.width * 0.5,  this.canvas.height * 0.25); }, 1800);
        setTimeout(() => { this.burst(this.canvas.width * 0.3,  this.canvas.height * 0.18); }, 2200);
        setTimeout(() => { this.burst(this.canvas.width * 0.7,  this.canvas.height * 0.28); }, 2500);
    };

    Fireworks.prototype.launchRocket = function () {
        this.rockets.push(new Rocket(this.colors));
    };

    Fireworks.prototype.burst = function (x, y) {
        this.bursts.push(new Burst(x, y, this.colors));
    };

    Fireworks.prototype.loop = function () {
        if (!this.running) return;
        const ctx = this.ctx;
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        ctx.globalAlpha = this.alpha;

        this.rockets = this.rockets.filter(r => r.active);
        this.bursts  = this.bursts.filter(b => b.active);

        this.rockets.forEach(r => { r.update(); r.draw(ctx); });
        this.bursts.forEach(b  => { b.update(); b.draw(ctx); });

        this.frame++;
        requestAnimationFrame(this.loop);
    };

    Fireworks.prototype.fadeOut = function () {
        const duration = 800;
        const start = performance.now();
        const self = this;
        function fade(now) {
            const elapsed = now - start;
            self.alpha = Math.max(0, 1 - elapsed / duration);
            if (self.alpha > 0) {
                requestAnimationFrame(fade);
            } else {
                self.running = false;
                self.canvas.remove();
                if (self.banner) self.banner.remove();
            }
        }
        requestAnimationFrame(fade);
    };

    Fireworks.prototype.showBanner = function () {
        const banner = document.createElement('div');
        banner.style.cssText = [
            'position:fixed',
            'top:50%',
            'left:50%',
            'transform:translate(-50%,-50%) scale(0)',
            'z-index:100000',
            'text-align:center',
            'pointer-events:none',
            'animation:fw-pop 0.5s cubic-bezier(0.34,1.56,0.64,1) 0.1s forwards, fw-fade 0.6s ease 2.4s forwards',
            'font-family:Inter,-apple-system,sans-serif'
        ].join(';');

        const gradText = this.colors.slice(0,3).join(',');
        banner.innerHTML = `
            <div style="font-size:3rem; margin-bottom:8px;">🎉 ✨ 🎊</div>
            <div style="font-size:1.8rem; font-weight:800; color:#fff;
                        text-shadow: 0 0 30px rgba(124,58,237,0.8), 0 2px 4px rgba(0,0,0,0.5);
                        background: linear-gradient(135deg, ${gradText});
                        -webkit-background-clip: text; -webkit-text-fill-color: transparent;
                        background-clip: text;">
                Welcome Back!
            </div>
            <div style="font-size:0.9rem; color:rgba(255,255,255,0.8); margin-top:6px;
                        text-shadow: 0 1px 3px rgba(0,0,0,0.5);">
                Login Successful
            </div>
        `;

        // Keyframes for banner
        if (!document.getElementById('fw-keyframes')) {
            const style = document.createElement('style');
            style.id = 'fw-keyframes';
            style.textContent = `
                @keyframes fw-pop {
                    from { transform: translate(-50%,-50%) scale(0); opacity:0; }
                    to   { transform: translate(-50%,-50%) scale(1); opacity:1; }
                }
                @keyframes fw-fade {
                    from { opacity: 1; transform: translate(-50%,-50%) scale(1); }
                    to   { opacity: 0; transform: translate(-50%,-50%) scale(0.85); }
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(banner);
        this.banner = banner;
    };

    // ---- Public API ----
    window.SecureEduFireworks = {
        launch: function () {
            const role = getRole();
            const colors = ROLE_COLORS[role] || ROLE_COLORS.default;
            return new Fireworks(colors);
        }
    };

})();
