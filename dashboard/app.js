// Reclaim impact dashboard: live Firestore data for admins, or clearly marked sample data.
import { addDays, computeMetrics, pitchSummary } from "./metrics.js";
import { sampleData } from "./sample.js";

const FIREBASE = "https://www.gstatic.com/firebasejs/12.17.1";
const today = new Date().toISOString().slice(0, 10);
const $ = (id) => document.getElementById(id);
const fmt = (n, digits = 0) => (n == null || Number.isNaN(n) ? "n/a" : n.toLocaleString("en-GB", { maximumFractionDigits: digits }));
const signed = (n) => (n == null ? "n/a" : `${n > 0 ? "+" : ""}${fmt(n)}`);

let source = "sample";
let firebase = null; // { auth, db, fns } once configured

// ---------- Rendering ----------

function tile(colour, value, label, detail, delay) {
  const el = document.createElement("div");
  el.className = "tile";
  el.style.background = `var(--${colour})`;
  el.style.animationDelay = `${delay * 60}ms`;
  el.innerHTML = `<div class="value"></div><div class="label"></div><div class="detail"></div>`;
  el.querySelector(".value").textContent = value;
  el.querySelector(".label").textContent = label;
  el.querySelector(".detail").textContent = detail;
  return el;
}

function renderTiles(m) {
  const tiles = [
    ["violet", fmt(m.users), "people sharing", `${fmt(m.active7)} active this week, ${fmt(m.new30)} new this month`],
    ["coral", `${signed(m.medianChangePercent)}%`, "median change in reels a day", `first week to latest, ${fmt(m.usersWithTwoWeeks)} people`],
    ["sunflower", `${fmt(m.cutByQuarterPercent)}%`, "cut reels by a quarter or more", "of people with two weeks or more"],
    ["sky", `${fmt(m.withinBudgetPercent)}%`, "days within their own budget", "last 30 days"],
    ["mint", `${fmt(m.pauseStopPercent)}%`, "mindful pauses that ended the session", `${fmt(m.pausesShown)} pauses shown`],
    ["pink", m.who5.people ? `${fmt(m.who5.first)} → ${fmt(m.who5.latest)}` : "n/a", "WHO-5 wellbeing, first to latest", `${fmt(m.who5.people)} people with 2+ check-ins`],
    ["violet", `${fmt(m.retentionD30.rate)}%`, "30-day retention", `7-day: ${fmt(m.retentionD7.rate)}%`],
    ["sunflower", fmt(m.hoursReclaimed), "hours won back", `${fmt(m.questsDone)} offline quests done`],
    ["mint", fmt(m.graduates), "graduates", `${fmt(m.graduationPercent)}% of people past 30 days`],
  ];
  $("tiles").replaceChildren(...tiles.map((t, i) => tile(...t, i)));
}

const svg = (w, h, inner) => `<svg viewBox="0 0 ${w} ${h}" role="img">${inner}</svg>`;

function lineChart(id, points, colour, label, W = 760) {
  const el = $(id);
  if (points.length < 2) {
    el.innerHTML = `<p class="hint">Not enough data yet.</p>`;
    return;
  }
  const H = W > 500 ? 240 : 200, L = 40, B = 24, T = 10;
  const xs = points.map((p) => p.x), ys = points.map((p) => p.y);
  const maxY = Math.max(...ys) * 1.1 || 1;
  const x = (i) => L + (i / (points.length - 1)) * (W - L - 10);
  const y = (v) => T + (1 - v / maxY) * (H - T - B);
  const path = points.map((p, i) => `${i ? "L" : "M"}${x(i).toFixed(1)},${y(p.y).toFixed(1)}`).join(" ");
  const area = `${path} L${x(points.length - 1)},${H - B} L${L},${H - B} Z`;
  const ticks = [0, 0.5, 1].map((f) => {
    const v = maxY * f;
    return `<line class="axis" x1="${L}" x2="${W - 10}" y1="${y(v)}" y2="${y(v)}"/><text x="${L - 6}" y="${y(v) + 4}" text-anchor="end">${fmt(v)}</text>`;
  }).join("");
  const labels = [0, Math.floor(points.length / 2), points.length - 1]
    .map((i) => `<text x="${x(i)}" y="${H - 6}" text-anchor="middle">${label(xs[i])}</text>`).join("");
  el.innerHTML = svg(W, H, `${ticks}${labels}
    <path d="${area}" fill="${colour}" opacity="0.15"/>
    <path class="draw" d="${path}" fill="none" stroke="${colour}" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>`);
  const line = el.querySelector(".draw");
  line.style.setProperty("--len", line.getTotalLength());
}

function barChart(id, bars) {
  const W = 360, H = 220, B = 26, gap = 24;
  const max = Math.max(...bars.map((b) => b.value), 1);
  const bw = (W - gap * (bars.length + 1)) / bars.length;
  const inner = bars.map((b, i) => {
    const h = (b.value / max) * (H - B - 26);
    const x = gap + i * (bw + gap);
    return `<rect class="grow" style="animation-delay:${i * 120}ms" x="${x}" y="${H - B - h}" width="${bw}" height="${h}" rx="14" fill="${b.colour}"/>
      <text x="${x + bw / 2}" y="${H - B - h - 8}" text-anchor="middle" style="font-weight:700">${b.text}</text>
      <text x="${x + bw / 2}" y="${H - 6}" text-anchor="middle">${b.label}</text>`;
  }).join("");
  $(id).innerHTML = svg(W, H, inner);
}

function render(data, sample) {
  const m = computeMetrics(data, today);
  $("sample-banner").hidden = !sample;
  $("asof").textContent = sample ? `Sample data, generated for ${today}` : `Live data, ${today}`;
  renderTiles(m);
  lineChart("chart-cohort", m.cohortCurve, "#6C4CF5", (d) => `day ${d}`);
  lineChart("chart-weekly", m.weeklyActive, "#34D1A0", (d) => d.slice(5), 380);
  barChart("chart-who5", m.who5.people
    ? [
        { label: "first", value: m.who5.first, text: fmt(m.who5.first), colour: "#FF94C6" },
        { label: "latest", value: m.who5.latest, text: fmt(m.who5.latest), colour: "#6C4CF5" },
      ]
    : []);
  const total = m.appSplit.instagram + m.appSplit.tiktok || 1;
  barChart("chart-apps", [
    { label: "Instagram", value: m.appSplit.instagram, text: `${fmt((m.appSplit.instagram / total) * 100)}%`, colour: "#FF94C6" },
    { label: "TikTok", value: m.appSplit.tiktok, text: `${fmt((m.appSplit.tiktok / total) * 100)}%`, colour: "#57B8FF" },
  ]);
  $("mood").innerHTML = m.moodRatio == null
    ? `<p class="hint">Not enough mood tags yet.</p>`
    : `<div class="big-ratio">${fmt(m.moodRatio, 1)}×</div><p>reels on low-mood days compared with other days, across ${fmt(m.moodDays)} tagged days.</p>`;
  $("pitch").textContent = pitchSummary(m, { sample });
}

// ---------- Data sources ----------

async function loadFirebase() {
  if (firebase) return firebase;
  let config;
  try {
    ({ firebaseConfig: config } = await import("./firebase-config.js"));
  } catch {
    return null;
  }
  const [{ initializeApp }, auth, fs] = await Promise.all([
    import(`${FIREBASE}/firebase-app.js`),
    import(`${FIREBASE}/firebase-auth.js`),
    import(`${FIREBASE}/firebase-firestore.js`),
  ]);
  const app = initializeApp(config);
  firebase = { auth: auth.getAuth(app), db: fs.getFirestore(app), authFns: auth, fs };
  return firebase;
}

async function loadLive() {
  const fb = await loadFirebase();
  if (!fb) {
    $("status").textContent = "No Firebase config yet: copy firebase-config.example.js to firebase-config.js. Showing sample data.";
    return showSample();
  }
  const user = fb.auth.currentUser;
  $("auth").hidden = false;
  if (!user) {
    $("auth").textContent = "Sign in with Google";
    $("status").textContent = "Sign in with an admin account to see live data.";
    return;
  }
  $("auth").textContent = "Sign out";
  $("status").textContent = "Loading live data...";
  const { collection, collectionGroup, getDocs, query, where } = fb.fs;
  try {
    const since = addDays(today, -180);
    const [users, days, checkins] = await Promise.all([
      getDocs(collection(fb.db, "users")),
      getDocs(query(collectionGroup(fb.db, "days"), where("date", ">=", since))),
      getDocs(collectionGroup(fb.db, "checkins")),
    ]);
    const owner = (doc) => doc.ref.parent.parent.id;
    render({
      users: users.docs.map((d) => ({ uid: d.id, ...d.data() })),
      days: days.docs.map((d) => ({ uid: owner(d), ...d.data() })),
      checkins: checkins.docs.map((d) => ({ uid: owner(d), ...d.data() })),
    }, false);
    $("status").textContent = users.empty ? "Connected. Nobody has opted in to sharing yet." : "";
  } catch (e) {
    $("status").textContent = e.code === "permission-denied"
      ? "This account isn't an admin. Add it to isAdmin() in firebase/firestore.rules."
      : `Couldn't load live data (${e.code || e.message}).`;
  }
}

function showSample() {
  source = "sample";
  $("src-sample").setAttribute("aria-pressed", "true");
  $("src-live").setAttribute("aria-pressed", "false");
  render(sampleData(today), true);
}

$("src-sample").addEventListener("click", () => {
  $("status").textContent = "";
  showSample();
});

$("src-live").addEventListener("click", async () => {
  source = "live";
  $("src-live").setAttribute("aria-pressed", "true");
  $("src-sample").setAttribute("aria-pressed", "false");
  const fb = await loadFirebase();
  if (fb && !fb.listening) {
    fb.listening = true;
    fb.authFns.onAuthStateChanged(fb.auth, () => source === "live" && loadLive());
  }
  loadLive();
});

$("auth").addEventListener("click", async () => {
  const fb = await loadFirebase();
  if (!fb) return;
  if (fb.auth.currentUser) await fb.authFns.signOut(fb.auth);
  else await fb.authFns.signInWithPopup(fb.auth, new fb.authFns.GoogleAuthProvider());
});

$("copy").addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText($("pitch").textContent);
    $("copy").textContent = "Copied";
    setTimeout(() => ($("copy").textContent = "Copy"), 1500);
  } catch {
    $("copy").textContent = "Select and copy";
  }
});

showSample();
