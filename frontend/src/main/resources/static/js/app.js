const API_URL = "http://localhost:8082/api/persons";
const SSE_URL = "http://localhost:8082/api/persons/stream";

// Thème
function toggleTheme() {
  const html = document.documentElement;
  const isDark = html.getAttribute("data-theme") === "dark";
  html.setAttribute("data-theme", isDark ? "light" : "dark");
  document.getElementById("theme-btn").textContent = isDark ? "☾" : "☀";
  localStorage.setItem("theme", isDark ? "light" : "dark");
}

// Restaurer le thème sauvegardé
const savedTheme = localStorage.getItem("theme") || "dark";
document.documentElement.setAttribute("data-theme", savedTheme);
document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("theme-btn").textContent =
    savedTheme === "dark" ? "☀" : "☾";
});

// Rendu d'une carte
function createCard(person, index) {
  const card = document.createElement("div");
  card.className = "card";
  card.dataset.id = person.id;
  card.style.animationDelay = `${index * 40}ms`;
  card.innerHTML = `
        <img src="${person.pictureUrl}" alt="${person.firstName}"
             onerror="this.src='https://randomuser.me/api/portraits/lego/1.jpg'">
        <div class="name">${person.firstName} ${person.lastName}</div>
        <div class="meta">${person.nationality}</div>
        <span class="age-badge">${person.age} ans</span>
    `;
  return card;
}

// Chargement initial
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

// SSE — écoute les nouvelles personnes en temps réel
function startSSE() {
  const es = new EventSource(SSE_URL);

  es.onmessage = (event) => {
    const person = JSON.parse(event.data);
    const grid = document.getElementById("grid");
    const count = document.getElementById("count");

    // Supprimer le message "aucune personne" si présent
    const empty = grid.querySelector(".empty");
    if (empty) empty.remove();

    // Ajouter la carte en tête de grille
    const card = createCard(person, 0);
    card.style.animationDelay = "0ms";
    grid.prepend(card);

    // Mettre à jour le compteur
    const current = parseInt(count.textContent) || 0;
    count.textContent = `${current + 1} personne${current + 1 !== 1 ? "s" : ""}`;
  };

  es.onerror = () => console.warn("SSE déconnecté, reconnexion automatique...");
}

loadPersons();
startSSE();
