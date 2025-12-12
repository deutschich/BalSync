const fs = require("fs");
const path = require("path");

// ---- Hilfsfunktion für sichere Fetches ----
async function safeFetchJson(url) {
  try {
    const res = await fetch(url);
    if (!res.ok) {
      console.error(`HTTP Error ${res.status} für URL: ${url}`);
      return null;
    }
    const text = await res.text();
    if (!text) {
      console.error(`Leerer Body für URL: ${url}`);
      return null;
    }
    return JSON.parse(text);
  } catch (e) {
    console.error(`Fehler beim Parsen von ${url}:`, e);
    return null;
  }
}

// ---- Modrinth Downloads ----
async function getModrinthDownloads(projectId) {
  const res = await fetch(`https://api.modrinth.com/v2/project/${projectId}`);
  if (!res.ok) return 0;
  const data = await res.json();
  return data.downloads ?? 0;
}

// ---- GitHub Downloads ----
async function getGithubDownloads(owner, repo) {
  const res = await fetch(`https://api.github.com/repos/${owner}/${repo}/releases`);
  if (!res.ok) return 0;
  const releases = await res.json();
  return releases.reduce(
    (sum, rel) => sum + rel.assets.reduce((a, asset) => a + asset.download_count, 0),
    0
  );
}

// ---- SpigotMC Downloads (HTML Parsing) ----
async function getSpigotDownloads(pluginId) {
  try {
    const url = `https://www.spigotmc.org/resources/${pluginId}/`;
    const res = await fetch(url);
    if (!res.ok) return 0;
    const text = await res.text();

    // Beispiel: "Downloads: 12,345"
    const match = text.match(/Total Downloads:\s*([\d,]+)/i);
    if (!match) return 0;

    return parseInt(match[1].replace(/,/g, ""), 10);
  } catch (e) {
    console.error("Fehler beim Abrufen von SpigotMC:", e);
    return 0;
  }
}

// ---- Badge JSON erzeugen ----
(async () => {
  const modrinth = await getModrinthDownloads("lWNvJAlY");
  const github = await getGithubDownloads("deutschich", "BalSync");
  const spigot = await getSpigotDownloads("130534"); // z. B. "12345"

  const total = modrinth + github + spigot;
  console.log(`Downloads: Modrinth=${modrinth}, GitHub=${github}, Spigot=${spigot}, Total=${total}`);

  const badgeJson = {
    schemaVersion: 1,
    label: "downloads",
    message: total.toString(),
    color: "blue",
  };

  // ---- Ordner prüfen und erstellen ----
  const badgeDir = path.join(__dirname, "..", "badges");
  if (!fs.existsSync(badgeDir)) {
    fs.mkdirSync(badgeDir, { recursive: true });
  }

  // ---- JSON speichern ----
  fs.writeFileSync(path.join(badgeDir, "downloads.json"), JSON.stringify(badgeJson, null, 2));
})();
