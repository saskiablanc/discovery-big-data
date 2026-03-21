const API_URL = "http://localhost:8082/api/persons";
const SSE_URL = "http://localhost:8082/api/persons/stream";

function toggleTheme() {
  const html = document.documentElement;
  const isDark = html.getAttribute("data-theme") === "dark";
  html.setAttribute("data-theme", isDark ? "light" : "dark");
  document.getElementById("theme-btn").textContent = isDark ? "☾" : "☀";
  localStorage.setItem("theme", isDark ? "light" : "dark");
}

const savedTheme = localStorage.getItem("theme") || "dark";
document.documentElement.setAttribute("data-theme", savedTheme);
document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("theme-btn").textContent =
    savedTheme === "dark" ? "☀" : "☾";
});

const ageGroupColor = {
  jeune: "#c8f542",
  adulte: "#42c8f5",
  senior: "#f5a742",
};

function createCard(person, index) {
  const card = document.createElement("div");
  card.className = "card";
  card.dataset.id = person.id;
  card.style.animationDelay = `${index * 40}ms`;
  const color = ageGroupColor[person.ageGroup] || "#888";
  card.innerHTML = `
        <img src="${person.pictureUrl}" alt="${person.firstName}"
             onerror="this.src='https://randomuser.me/api/portraits/lego/1.jpg'">
        <div class="name">${person.firstName} ${person.lastName}</div>
        <div class="meta">${person.nationality}</div>
        <div style="display:flex;gap:6px;align-items:center;justify-content:center">
            <span class="age-badge">${person.age} ans</span>
            <span class="age-badge" style="background:${color};color:#0f0f0f">${person.ageGroup}</span>
        </div>
    `;
  return card;
}

async function loadPersons() {
  const grid = document.getElementById("grid");
  const count = document.getElementById("count");
  try {
    const res = await fetch(API_URL);
    const persons = await res.json();
    grid.innerHTML = "";
    count.textContent = `${persons.length} personne${persons.length !== 1 ? "s" : ""}`;
    if (persons.length === 0) {
      grid.innerHTML = '<div class="empty">Aucune personne en base.</div>';
      return;
    }
    persons.forEach((p, i) => grid.appendChild(createCard(p, i)));
  } catch (e) {
    grid.innerHTML = '<div class="empty">Impossible de contacter l\'API.</div>';
    console.error(e);
  }
}

function startSSE() {
  const es = new EventSource(SSE_URL);
  es.onmessage = (event) => {
    const person = JSON.parse(event.data);
    const grid = document.getElementById("grid");
    const count = document.getElementById("count");
    const empty = grid.querySelector(".empty");
    if (empty) empty.remove();
    const card = createCard(person, 0);
    card.style.animationDelay = "0ms";
    grid.prepend(card);
    const current = parseInt(count.textContent) || 0;
    count.textContent = `${current + 1} personne${current + 1 !== 1 ? "s" : ""}`;
  };
  es.onerror = () => console.warn("SSE déconnecté, reconnexion automatique...");
}

loadPersons();
startSSE();
