/* ═══════════════════════════════════════════════════════════
   ForgeMind PC Dashboard — Local Mode
   ═══════════════════════════════════════════════════════════ */

let BACKEND = getBackendBaseUrl();

const state = {
  connected: false,
  simulating: false,
  simInterval: null,
  simScenario: 'healthy',
  telemetry: { temp_c: 0, current_a: 0, rpm: 0, anomaly_score: 0 },
  history: { temp_c: [], current_a: [], rpm: [], anomaly_score: [] },
  activeMetric: 'temp_c',
  totalDiagnoses: 0,
  hasPopupShown: false
};
// Notification events captured locally for the UI panel
const notificationEvents = [];
const MAX_HISTORY = 60;

const KB = [
  { id: "bent_blade", title: "Bent Fan Blade", symptoms: "Periodic scraping sound. Vision shows bent blade.", root_cause: "Deformed blades contacting housing.", repair: ["Power down.", "Remove grille.", "Straighten blade."], safety: ["Lockout power.", "Wear eye protection."], tools: ["Pliers", "Screwdriver"] }
];

const SIM = {
  healthy: { temp: [35, 42], cur: [0.30, 0.40], rpm: [2400, 2600], anom: [0.2, 1.0] },
  bent_blade: { temp: [42, 50], cur: [0.38, 0.48], rpm: [2300, 2500], anom: [5.0, 8.0] },
  dust_buildup: { temp: [48, 56], cur: [0.35, 0.42], rpm: [2100, 2350], anom: [2.5, 4.5] },
  worn_bearing: { temp: [50, 62], cur: [0.40, 0.65], rpm: [1800, 2200], anom: [4.0, 7.0] },
  loose_mount: { temp: [36, 43], cur: [0.32, 0.40], rpm: [2350, 2550], anom: [5.5, 8.5] },
  motor_overload: { temp: [58, 80], cur: [0.70, 1.50], rpm: [800, 1600], anom: [7.0, 10.0] },
};

const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);
const rand = (a, b) => a + Math.random() * (b - a);
const now = () => new Date().toLocaleTimeString('en-US', { hour12: false });
const sleep = ms => new Promise(r => setTimeout(r, ms));

function getBackendBaseUrl() {
  const stored = localStorage.getItem('forgemind.backendUrl')?.trim();
  if (stored) return stored.replace(/\/$/, '');

  if (window.location.hostname) {
    return `http://${window.location.hostname}:8000`;
  }

  return 'http://localhost:8000';
}

function setBackendUrlDisplay(url) {
  const display = $('#backendUrlDisplay');
  if (display) display.textContent = url;
}

function persistBackendUrl(url) {
  const normalized = url.replace(/\/$/, '');
  localStorage.setItem('forgemind.backendUrl', normalized);
  BACKEND = normalized;
  setBackendUrlDisplay(normalized);
  return normalized;
}

document.addEventListener('DOMContentLoaded', () => {
  initNav();
  initClock();
  initChart();
  initSettings();
  initAnalysis();
  initNotifications();
  initChat();
  BACKEND = getBackendBaseUrl();
  setBackendUrlDisplay(BACKEND);
  checkBackend();
});

// ═══════════ NAVIGATION ═══════════
function initNav() {
  $$('.nav-item').forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      gotoPage(item.dataset.page);
    });
  });
  $('#menuToggle').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
  $('#reconnectBtn').addEventListener('click', () => gotoPage('settings'));

  $('#dismissAnomalyBtn').addEventListener('click', () => $('#anomalyPopup').classList.remove('show'));
  $('#analyzeAnomalyBtn').addEventListener('click', () => {
    $('#anomalyPopup').classList.remove('show');
    gotoPage('incident');
  });

  const sendPhoneBtn = $('#topbarSendPhone');
  if (sendPhoneBtn) {
    sendPhoneBtn.addEventListener('click', async () => {
      const btn = sendPhoneBtn;
      const originalText = btn.innerHTML;
      btn.innerHTML = '<span class="spinner" style="width:14px;height:14px;border-width:2px;margin-right:6px;"></span> Sending...';
      const success = await notifyPhone('Manual Review Requested', `Operator requested phone LLM review for FAN-01.`, 'Info', true);
      btn.innerHTML = originalText;
      if (success) showToast('Sent to Phone', 'Data successfully pushed to phone.', 'success');
      else showToast('Error', 'Failed to connect to backend.', 'error');
    });
  }
}

function gotoPage(page) {
  $$('.nav-item').forEach(n => n.classList.remove('active'));
  $(`#nav-${page}`).classList.add('active');
  $$('.page').forEach(p => p.classList.remove('active'));
  $(`#page-${page}`).classList.add('active');
  $('#pageTitle').textContent = $(`#nav-${page} span`).textContent;
  $('#sidebar').classList.remove('open');
}

// ═══════════ CLOCK & BACKEND ═══════════
function initClock() {
  const tick = () => { $('#topbarClock').textContent = now(); };
  tick(); setInterval(tick, 1000);
}

async function checkBackend(showResult = false) {
  const r = $('#connectionTestResult');
  try {
    const res = await fetch(`${BACKEND}/health`, { signal: AbortSignal.timeout(3000) });
    const d = await res.json();
    if (d.status === 'healthy') {
      state.connected = true; setConn(true);
      if (showResult) { r.className = 'connection-test-result success'; r.textContent = '✓ Connected'; }
    }
  } catch {
    state.connected = false; setConn(false);
    if (showResult) { r.className = 'connection-test-result error'; r.textContent = '✗ Backend offline'; }
  }
}
function setConn(on) {
  const d = $('#connectionStatus .status-dot'), t = $('#connectionStatus .status-text'), bd = $('#backendTag .status-dot-sm');
  d.className = on ? 'status-dot online' : 'status-dot offline';
  t.textContent = on ? 'Backend Online' : 'Backend Offline';
  if (bd) bd.className = on ? 'status-dot-sm online' : 'status-dot-sm offline';
}

// ═══════════ TELEMETRY & SIMULATION ═══════════
function initSettings() {
  const backendInput = $('#settingsBackendUrl');
  if (backendInput) {
    backendInput.value = BACKEND;
    backendInput.addEventListener('change', () => {
      persistBackendUrl(backendInput.value || BACKEND);
      checkBackend(true);
    });
  }

  $('#testConnectionBtn').addEventListener('click', () => {
    const inputValue = backendInput?.value?.trim() || BACKEND;
    persistBackendUrl(inputValue);
    checkBackend(true);
  });

  const sim = $('#simToggle');
  sim.addEventListener('change', () => {
    if (sim.checked) { $('#simControls').classList.remove('hidden'); startSim(); }
    else { $('#simControls').classList.add('hidden'); stopSim(); }
  });
  $('#simScenario').addEventListener('change', e => {
    state.simScenario = e.target.value;
    state.hasPopupShown = false; // Reset popup logic on scenario change
    if (state.simulating) { stopSim(); startSim(); }
  });
}

function startSim() {
  if (state.simInterval) clearInterval(state.simInterval);
  state.simulating = true;
  state.simInterval = setInterval(() => {
    const p = SIM[state.simScenario];
    feed({ temp_c: +rand(...p.temp).toFixed(1), current_a: +rand(...p.cur).toFixed(2), rpm: Math.round(rand(...p.rpm)), anomaly_score: +rand(...p.anom).toFixed(1) });
  }, 1000);
}
function stopSim() {
  if (state.simInterval) { clearInterval(state.simInterval); state.simInterval = null; }
  state.simulating = false;
}

function feed(d) {
  state.telemetry = d;
  $('#snapTemp').textContent = d.temp_c + '°C'; $('#snapCurrent').textContent = d.current_a + 'A';
  $('#snapRpm').textContent = d.rpm; $('#snapAnomaly').textContent = d.anomaly_score.toFixed(1);
  updateGauges(d);

  for (const k of ['temp_c', 'current_a', 'rpm', 'anomaly_score']) {
    state.history[k].push(d[k]);
    if (state.history[k].length > MAX_HISTORY) state.history[k].shift();
  }
  draw();
  $('#telemetryBadge').innerHTML = '<span class="live-dot"></span> Live';

  const icon = $('#statusOverall .status-card-icon'), txt = $('#machineStatusText');
  const topbarBtn = $('#topbarSendPhone');
  if (d.anomaly_score > 5) {
    icon.className = 'status-card-icon danger'; txt.textContent = 'Anomaly Detected'; txt.style.color = 'var(--red)';
    if (topbarBtn) topbarBtn.classList.add('pulse');
    if (!state.hasPopupShown) {
      $('#anomalyPopup').classList.add('show');
      state.hasPopupShown = true;

      notifyPhone('Critical Anomaly Detected', `Anomaly score ${d.anomaly_score} on ${now()}. Immediate attention required.`, 'High', true)
        .then(success => { if (success) showToast('Auto-Alert Sent', 'Notification pushed to phone.', 'warning'); });

      // push a notification event and show a browser notification
      const note = { title: 'Anomaly Detected', text: `Anomaly score ${d.anomaly_score} on ${now()}`, time: Date.now(), data: { telemetry: d } };
      pushNotificationEvent(note);
      showBrowserNotification(note.title, note.text);
    }
  } else if (d.anomaly_score > 3) {
    icon.className = 'status-card-icon warning'; txt.textContent = 'Warning'; txt.style.color = 'var(--amber)';
    if (topbarBtn) topbarBtn.classList.remove('pulse');
  } else {
    icon.className = 'status-card-icon healthy'; txt.textContent = 'Healthy'; txt.style.color = 'var(--green)';
    if (topbarBtn) topbarBtn.classList.remove('pulse');
  }
}

// ═══════════ GAUGES & CHART ═══════════
const C = 2 * Math.PI * 52;
function setGauge(ring, val, value, max) {
  $(`#${ring}`).style.strokeDashoffset = C * (1 - Math.min(value / max, 1));
  $(`#${val}`).textContent = typeof value === 'number' ? (Number.isInteger(value) ? value : value.toFixed(1)) : '--';
}
function updateGauges(t) {
  setGauge('tempRing', 'tempValue', t.temp_c, 100); setGauge('currentRing', 'currentValue', t.current_a, 2.0);
  setGauge('rpmRing', 'rpmValue', t.rpm, 5000); setGauge('anomalyRing', 'anomalyValue', t.anomaly_score, 10);
  $('#anomalyRing').style.stroke = t.anomaly_score > 5 ? '#ef4444' : t.anomaly_score > 3 ? '#f59e0b' : '#22c55e';
}

let ctx, canvas;
const COLORS = { temp_c: '#f97316', current_a: '#3b82f6', rpm: '#22c55e', anomaly_score: '#ef4444' };
const RANGES = { temp_c: [0, 100], current_a: [0, 2], rpm: [0, 5000], anomaly_score: [0, 10] };
function initChart() {
  canvas = $('#telemetryChart'); ctx = canvas.getContext('2d');
  $$('.chart-btn').forEach(b => b.addEventListener('click', () => {
    $$('.chart-btn').forEach(x => x.classList.remove('active'));
    b.classList.add('active'); state.activeMetric = b.dataset.metric; draw();
  }));
  new ResizeObserver(() => draw()).observe(canvas.parentElement); draw();
}
function draw() {
  if (!canvas.parentElement || canvas.parentElement.clientWidth === 0) return;
  const p = canvas.parentElement, dpr = devicePixelRatio || 1;
  const w = p.clientWidth - 40, h = p.clientHeight - 40;
  canvas.width = w * dpr; canvas.height = h * dpr; canvas.style.width = w + 'px'; canvas.style.height = h + 'px';
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  const data = state.history[state.activeMetric], color = COLORS[state.activeMetric], [minY, maxY] = RANGES[state.activeMetric];
  const L = 50, T = 20, R = 20, B = 30, cw = w - L - R, ch = h - T - B;
  ctx.clearRect(0, 0, w, h);
  ctx.strokeStyle = 'rgba(255,255,255,0.04)'; ctx.lineWidth = 1;
  for (let i = 0; i <= 5; i++) {
    const y = T + (ch / 5) * i; ctx.beginPath(); ctx.moveTo(L, y); ctx.lineTo(w - R, y); ctx.stroke();
    ctx.fillStyle = 'rgba(255,255,255,0.25)'; ctx.font = '11px "JetBrains Mono",monospace'; ctx.textAlign = 'right';
    ctx.fillText((maxY - ((maxY - minY) / 5) * i).toFixed(state.activeMetric === 'rpm' ? 0 : 1), L - 8, y + 4);
  }
  if (data.length < 2) { ctx.fillStyle = 'rgba(255,255,255,0.15)'; ctx.font = '13px Inter,sans-serif'; ctx.textAlign = 'center'; ctx.fillText('Waiting for data…', w / 2, h / 2); return; }
  const sx = cw / (MAX_HISTORY - 1), gy = v => T + ch - ((v - minY) / (maxY - minY)) * ch;
  const gr = ctx.createLinearGradient(0, T, 0, h - B); gr.addColorStop(0, color + '30'); gr.addColorStop(1, color + '00');
  ctx.beginPath(); data.forEach((v, i) => { const x = L + (MAX_HISTORY - data.length + i) * sx; i === 0 ? ctx.moveTo(x, gy(v)) : ctx.lineTo(x, gy(v)); });
  ctx.lineTo(L + (MAX_HISTORY - 1) * sx, h - B); ctx.lineTo(L + (MAX_HISTORY - data.length) * sx, h - B);
  ctx.closePath(); ctx.fillStyle = gr; ctx.fill();
  ctx.beginPath(); data.forEach((v, i) => { const x = L + (MAX_HISTORY - data.length + i) * sx; i === 0 ? ctx.moveTo(x, gy(v)) : ctx.lineTo(x, gy(v)); });
  ctx.strokeStyle = color; ctx.lineWidth = 2.5; ctx.lineJoin = 'round'; ctx.stroke();
  ctx.shadowColor = color; ctx.shadowBlur = 12; ctx.stroke(); ctx.shadowBlur = 0;
  const lx = L + (MAX_HISTORY - 1) * sx, ly = gy(data[data.length - 1]);
  ctx.beginPath(); ctx.arc(lx, ly, 4, 0, Math.PI * 2); ctx.fillStyle = color; ctx.fill();
  ctx.beginPath(); ctx.arc(lx, ly, 7, 0, Math.PI * 2); ctx.strokeStyle = color + '40'; ctx.lineWidth = 2; ctx.stroke();
}

// ═══════════ INCIDENT ANALYSIS ═══════════
let lastPayloadTimestamp = 0;
function initAnalysis() {

  // File inputs for sending
  $('#imageInput').addEventListener('change', e => {
    const file = e.target.files[0];
    if (file) {
      $('#imagePreview').src = URL.createObjectURL(file);
      $('#imagePreview').style.display = 'block';
      $('#statusImage').textContent = "Ready to send: " + file.name;
      $('#statusImage').classList.add('success');
    }
  });

  $('#audioInput').addEventListener('change', e => {
    const file = e.target.files[0];
    if (file) {
      $('#audioPreview').src = URL.createObjectURL(file);
      $('#audioPreview').style.display = 'block';
      $('#statusAudio').textContent = "Ready to send: " + file.name;
      $('#statusAudio').classList.add('success');
    }
  });

  // Poll latest payload from backend to view things uploaded by others
  setInterval(async () => {
    if (!state.connected) return;
    try {
      const res = await fetch(`${BACKEND}/latest-payload`);
      const payload = await res.json();
      if (payload.timestamp > lastPayloadTimestamp) {
        lastPayloadTimestamp = payload.timestamp;
        if (payload.image && !$('#imageInput').files.length) {
          $('#imagePreview').src = `${BACKEND}/${payload.image}`;
          $('#imagePreview').style.display = 'block';
          $('#statusImage').textContent = "Received remote payload: " + payload.image.split('/').pop();
          $('#statusImage').classList.add('success');
        }
        if (payload.audio && !$('#audioInput').files.length) {
          $('#audioPreview').src = `${BACKEND}/${payload.audio}`;
          $('#audioPreview').style.display = 'block';
          $('#statusAudio').textContent = "Received remote payload: " + payload.audio.split('/').pop();
          $('#statusAudio').classList.add('success');
        }
        if (payload.telemetry) {
          $('#snapTemp').textContent = payload.telemetry.temperature + '°C';
          $('#snapCurrent').textContent = payload.telemetry.current + 'A';
          $('#snapRpm').textContent = payload.telemetry.rpm;
          $('#snapAnomaly').textContent = payload.telemetry.anomaly_score.toFixed(1);
          state.telemetry = { temp_c: payload.telemetry.temperature, current_a: payload.telemetry.current, rpm: payload.telemetry.rpm, anomaly_score: payload.telemetry.anomaly_score };
        }
      }
    } catch (e) { }
  }, 2000);

  $('#analyzeBtn').addEventListener('click', async () => {
    const btn = $('#analyzeBtn');
    btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Packaging Anomaly Payload...';
    $('#diagnosisResult').classList.add('hidden');

    // Animate pipeline
    const stages = ['stage-vision', 'stage-audio', 'stage-rag', 'stage-gemma'];
    for (const s of stages) $(`#${s}`).className = 'pipe-stage idle';
    for (const s of stages) {
      $(`#${s}`).className = 'pipe-stage running';
      await sleep(400 + Math.random() * 300);
      $(`#${s}`).className = 'pipe-stage done';
    }

    try {
      const fd = new FormData();
      fd.append('temperature', state.telemetry.temp_c || 45);
      fd.append('current', state.telemetry.current_a || 0.4);
      fd.append('rpm', state.telemetry.rpm || 2400);
      fd.append('anomaly_score', state.telemetry.anomaly_score || 0);

      const imgFile = $('#imageInput').files[0];
      if (imgFile) fd.append('image', imgFile);

      const audioFile = $('#audioInput').files[0];
      if (audioFile) fd.append('audio', audioFile);

      const res = await fetch(`${BACKEND}/analyze`, { method: 'POST', body: fd });
      if (!res.ok) throw new Error('API Error');
      const d = await res.json();
      // centralized display logic
      displayDiagnosis(d);

      const statusBadge = $('#phoneSentStatus');
      if (statusBadge) statusBadge.style.display = 'inline-flex';
      showToast('Payload Sent', 'Incident payload and diagnosis sent to phone LLM.', 'success');
    } catch (err) {
      $('#resultFault').textContent = 'Connection Error';
      $('#resultSeverity').textContent = 'Critical';
      $('#resultSeverity').className = 'severity-badge Critical';
      $('#resultSummary').textContent = 'Failed to reach FastAPI backend.';
      $('#resultRecommendation').textContent = `Ensure the backend is running and reachable at ${BACKEND}`;
      $('#diagnosisResult').classList.remove('hidden');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> Send Anomaly Payload to Phone LLM';
    }
  });
}

function addHistory(d) {
  const c = $('#diagnosisHistory');
  const empty = c.querySelector('.event-empty'); if (empty) empty.remove();
  const el = document.createElement('div'); el.className = 'history-item';
  el.innerHTML = `<span class="severity-badge ${d.severity}" style="font-size:0.65rem;padding:2px 8px;">${d.severity}</span><span class="history-fault">${d.fault}</span><span class="history-confidence">${d.confidence}%</span><span class="history-time">${now()}</span>`;
  c.prepend(el);
  // Also push into notification events for quick access
  const note = { title: d.fault, text: d.summary || `Severity ${d.severity} • ${d.confidence}%`, time: Date.now(), data: d };
  pushNotificationEvent(note);
  // Show browser notification for high-severity or high-confidence diagnoses
  try {
    if (d.severity === 'High' || (d.confidence && d.confidence >= 75)) {
      showBrowserNotification(note.title, note.text);
      // show anomaly popup for immediate attention
      const popup = $('#anomalyPopup'); if (popup) popup.classList.add('show');
    }
  } catch (e) { }
}

function displayDiagnosis(d) {
  $('#resultFault').textContent = d.fault;
  $('#resultSeverity').textContent = d.severity;
  $('#resultSeverity').className = `severity-badge ${d.severity}`;
  $('#resultConfidenceBar').style.setProperty('--confidence', `${d.confidence}%`);
  $('#resultConfidence').textContent = `${d.confidence}%`;
  $('#resultSummary').textContent = "Payload sent to Snapdragon Phone for LLM reasoning. Result: " + (d.summary || 'N/A');
  $('#resultRecommendation').textContent = d.recommendation || '';
  $('#diagnosisResult').classList.remove('hidden');

  state.totalDiagnoses++; $('#totalDiagnoses').textContent = state.totalDiagnoses;
  addHistory(d);
}

/* Notifications UI & Browser Notification helpers */
function initNotifications() {
  // request permission for browser notifications
  if ('Notification' in window && Notification.permission !== 'granted') {
    try { Notification.requestPermission(); } catch (e) { }
  }

  const btn = $('#notifBtn');
  const panel = $('#notifPanel');
  // load persisted notifications
  try {
    const stored = localStorage.getItem('forgemind.notifications');
    if (stored) {
      const arr = JSON.parse(stored);
      if (Array.isArray(arr) && arr.length) {
        notificationEvents.splice(0, notificationEvents.length, ...arr);
        const badge = $('#notifBadge'); if (badge) badge.style.display = 'inline-block';
      }
    }
  } catch (e) { }
  btn?.addEventListener('click', (e) => {
    panel.classList.toggle('hidden');
    renderNotifPanel();
  });
  document.addEventListener('click', (e) => {
    if (!e.target.closest || !e.target.closest('#topbarNotif')) {
      panel.classList.add('hidden');
    }
  });
}

function updateNotificationState() {
  const badge = $('#notifBadge');
  if (badge) {
    if (notificationEvents.length > 0) {
      badge.style.display = 'inline-flex';
      badge.style.alignItems = 'center';
      badge.style.justifyContent = 'center';
      badge.style.fontSize = '8px';
      badge.style.color = 'white';
      badge.style.fontWeight = 'bold';
      badge.textContent = notificationEvents.length > 9 ? '9+' : notificationEvents.length;
    } else {
      badge.style.display = 'none';
    }
  }
  try { localStorage.setItem('forgemind.notifications', JSON.stringify(notificationEvents)); } catch (e) { }
  renderNotifPanel();
}

function pushNotificationEvent(ev) {
  if (ev) {
    notificationEvents.unshift(ev);
    if (notificationEvents.length > 20) notificationEvents.pop();
  }
  updateNotificationState();
}

function renderNotifPanel() {
  const panel = $('#notifPanel'); if (!panel) return;
  panel.innerHTML = '';
  if (notificationEvents.length === 0) { panel.innerHTML = '<div style="padding:12px;color:var(--text-muted)">No notifications</div>'; return; }

  const header = document.createElement('div');
  header.innerHTML = '<div style="display:flex; justify-content:space-between; align-items:center; padding:8px 12px; border-bottom:1px solid var(--border);"><span style="font-size:0.85rem; font-weight:600">Notifications</span><button id="clearNotifsBtn" class="btn-ghost" style="padding:4px 8px; font-size:0.75rem">Clear All</button></div>';
  panel.appendChild(header);
  header.querySelector('#clearNotifsBtn').addEventListener('click', (e) => {
    e.stopPropagation();
    notificationEvents.length = 0;
    updateNotificationState();
  });

  notificationEvents.slice(0, 10).forEach((n, idx) => {
    const isCritical = n.data && (n.data.anomaly_score > 5 || n.data.severity === 'High' || n.data.severity === 'Critical');
    const isWarning = n.data && (n.data.anomaly_score > 3 || n.data.severity === 'Medium' || n.data.severity === 'Warning');
    const iconSvg = isCritical ?
      `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--red)" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>` :
      (isWarning ? `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--amber)" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>` :
        `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`);

    const el = document.createElement('div'); el.className = 'notif-item'; el.style.position = 'relative';
    el.innerHTML = `<div style="margin-top:2px">${iconSvg}</div><div style="flex:1; padding-right:24px"><div class=\"ni-title\">${n.title}</div><div class=\"ni-text\">${n.text}</div></div><div class=\"ni-time\">${new Date(n.time).toLocaleTimeString()}</div><button class="dismiss-notif" style="position:absolute; right:8px; top:8px; background:none; border:none; color:var(--text-muted); cursor:pointer; opacity:0.6"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>`;

    el.querySelector('.dismiss-notif').addEventListener('click', (e) => {
      e.stopPropagation();
      notificationEvents.splice(idx, 1);
      updateNotificationState();
    });

    el.addEventListener('click', () => { // replay / show details
      // if this notification contains a diagnosis data object, show it
      if (n.data && n.data.fault) {
        displayDiagnosis(n.data);
      } else if (n.data && n.data.telemetry) {
        // construct a fake diagnosis to show
        const fake = { fault: 'Simulated Anomaly', confidence: 85, severity: 'High', summary: 'Simulated from telemetry', recommendation: 'Inspect machine' };
        displayDiagnosis(fake);
      }
      // hide panel
      panel.classList.add('hidden');
    });
    panel.appendChild(el);
  });
}

function showBrowserNotification(title, text) {
  if (!('Notification' in window)) return;
  try {
    if (Notification.permission === 'granted') {
      new Notification(title, { body: text, icon: null });
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission().then(p => { if (p === 'granted') new Notification(title, { body: text }); });
    }
  } catch (e) { }
}

// ═══════════ CHAT UI ═══════════
function initChat() {
  $('#chatSendBtn').addEventListener('click', sendChat);
  $('#chatInput').addEventListener('keypress', e => { if (e.key === 'Enter') sendChat(); });
}
async function sendChat() {
  const inp = $('#chatInput'), text = inp.value.trim();
  if (!text) return;
  inp.value = '';
  addChatMsg('user', text);

  // Mock AI response delay
  await sleep(800);
  addChatMsg('ai', "I'm analyzing your request based on the recent telemetry and diagnosis data. The system shows we had a bent blade detection. How can I assist you with the repair?");
}
function addChatMsg(role, text) {
  const box = $('#chatMessages');
  const el = document.createElement('div');
  el.className = `chat-msg ${role}`;
  el.innerHTML = `<div class="msg-bubble">${text}</div>`;
  box.appendChild(el);
  box.scrollTop = box.scrollHeight;
}

// ═══════════ TOAST & NOTIFY ═══════════
function showToast(title, text, type = 'info') {
  const container = $('#toastContainer');
  if (!container) return;
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  t.innerHTML = `
    <div class="toast-icon">
      ${type === 'success' ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>'
      : type === 'error' ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>'
        : type === 'warning' ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
          : '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'}
    </div>
    <div class="toast-body">
      <div class="toast-title">${title}</div>
      <div class="toast-text">${text}</div>
    </div>
    <button class="toast-close"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
  `;
  t.querySelector('.toast-close').addEventListener('click', () => t.remove());
  container.appendChild(t);
  setTimeout(() => { if (t.parentNode) t.remove(); }, 4000);
}

async function notifyPhone(title, message, severity = 'Info', includeTelemetry = true) {
  if (!state.connected) return false;
  try {
    const payload = { title, message, severity, machine_id: 'FAN-01' };
    if (includeTelemetry && state.telemetry) {
      payload.temperature = state.telemetry.temp_c;
      payload.current = state.telemetry.current_a;
      payload.rpm = state.telemetry.rpm;
      payload.anomaly_score = state.telemetry.anomaly_score;
    }
    const res = await fetch(`${BACKEND}/notify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return res.ok;
  } catch (e) {
    return false;
  }
}
