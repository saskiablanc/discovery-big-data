const API_URL = "http://localhost:8082/api/persons";
const SSE_URL = "http://localhost:8082/api/persons/stream";
const PAGE_SIZE = 50;

let currentPage = 0;
let totalElements = 0;
let loading = false;
let allLoaded = false;
let firstPageLoaded = false;

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

function createCard(person, index) {
  const card = document.createElement("div");
  card.className = "card";
  card.dataset.id = person.id;
  card.style.animationDelay = `${index * 20}ms`;
  card.innerHTML = `
    <img src="${person.pictureUrl}" alt="${person.firstName}"
         onerror="this.src='https://randomuser.me/api/portraits/lego/1.jpg'">
    <div class="name">${person.firstName} ${person.lastName}</div>
    <div class="meta">${person.nationality}</div>
    <div style="display:flex;gap:6px;align-items:center;justify-content:center">
      <span class="age-badge">${person.age} ans</span>
    </div>
  `;
  return card;
}

function updateCount() {
  const displayed = document
    .getElementById("grid")
    .querySelectorAll(".card").length;
  const count = document.getElementById("count");
  count.textContent =
    totalElements > 0
      ? `${displayed} / ${totalElements} personne${totalElements !== 1 ? "s" : ""}`
      : `${displayed} personne${displayed !== 1 ? "s" : ""}`;
}

async function loadPage() {
  if (loading || allLoaded) return;
  loading = true;

  const grid = document.getElementById("grid");

  try {
    const res = await fetch(`${API_URL}?page=${currentPage}&size=${PAGE_SIZE}`);
    const data = await res.json();

    totalElements = data.totalElements;

    const empty = grid.querySelector(".empty");
    if (empty) empty.remove();

    if (data.content.length === 0 && currentPage === 0) {
      grid.innerHTML = '<div class="empty">En attente de données...</div>';
      loading = false;
      return;
    }

    data.content.forEach((p, i) => grid.appendChild(createCard(p, i)));
    updateCount();

    currentPage++;
    firstPageLoaded = true;
    if (currentPage >= data.totalPages) allLoaded = true;
  } catch (e) {
    if (currentPage === 0) {
      grid.innerHTML =
        '<div class="empty">Impossible de contacter l\'API.</div>';
    }
    console.error(e);
  }

  loading = false;
}

function startSSE() {
  const es = new EventSource(SSE_URL);
  es.onmessage = (event) => {
    const data = JSON.parse(event.data);
    totalElements += data.count;
    updateCount();

    if (!firstPageLoaded) {
      loadPage();
    }
  };
  es.onerror = () => console.warn("SSE déconnecté, reconnexion automatique...");
}

// Scroll infini
window.addEventListener("scroll", () => {
  if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 300) {
    loadPage();
  }
});

loadPage();
startSSE();
