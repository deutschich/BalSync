const fs = require("fs");
const path = require("path");

// ---- Hilfsfunktion für sichere Fetches ----
async function safeFetch(url, options = {}) {
  try {
    const res = await fetch(url, options);
    if (!res.ok) {
      console.error(`HTTP Error ${res.status} für URL: ${url}`);
      return null;
    }
    return await res.text();
  } catch (e) {
    console.error(`Fetch Fehler für ${url}:`, e);
    return null;
  }
}

async function safeFetchJson(url, options = {}) {
  const text = await safeFetch(url, options);
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch (e) {
    console.error(`JSON Parse Fehler für ${url}:`, e);
    return null;
  }
}

// ---- Modrinth Downloads ----
async function getModrinthDownloads(projectId) {
  const data = await safeFetchJson(`https://api.modrinth.com/v2/project/${projectId}`);
  return data?.downloads ?? 0;
}

// ---- GitHub Downloads ----
async function getGithubDownloads(owner, repo) {
  const releases = await safeFetchJson(
    `https://api.github.com/repos/${owner}/${repo}/releases`
  );

  if (!releases) return 0;

  return releases.reduce(
    (sum, rel) =>
      sum +
      rel.assets.reduce((assetSum, asset) => assetSum + (asset.download_count || 0), 0),
    0
  );
}

// ---- SpigotMC Downloads (HTML Parsing) ----
async function getSpigotDownloads(pluginId) {
  const url = `https://www.spigotmc.org/resources/${pluginId}/`;
  const html = await safeFetch(url);

  if (!html) return 0;

  const match = html.match(/Total Downloads:\s*([\d,]+)/i);
  if (!match) return 0;

  return parseInt(match[1].replace(/,/g, ""), 10);
}

// ---- CurseForge Downloads (HTML Parsing, Plugin) ----
async function getCurseForgeDownloads(slug) {
  const url = `https://www.curseforge.com/minecraft/bukkit-plugins/${slug}`;

  const html = await safeFetch(url, {
    headers: {
      "User-Agent": "Mozilla/5.0",
    },
  });

  if (!html) return 0;

  const match = html.match(/All-time Downloads[^0-9]*([\d,]+)/i);
  if (!match) return 0;

  return parseInt(match[1].replace(/,/g, ""), 10);
}

// ---- Badge JSON erzeugen ----
(async () => {
  try {
    const [modrinth, github, spigot, curseforge] = await Promise.all([
      getModrinthDownloads("lWNvJAlY"),
      getGithubDownloads("deutschich", "BalSync"),
      getSpigotDownloads("130534"),
      getCurseForgeDownloads("balsync"), // <-- Plugin Slug
    ]);

    const total = modrinth + github + spigot + curseforge;

    console.log(
      `Downloads: Modrinth=${modrinth}, GitHub=${github}, Spigot=${spigot}, CurseForge=${curseforge}, Total=${total}`
    );

    const badgeJson = {
      schemaVersion: 1,
      label: "downloads",
      message: total.toString(),
      color: "blue",
    };

    const badgeDir = path.join(__dirname, "..", "badges");

    if (!fs.existsSync(badgeDir)) {
      fs.mkdirSync(badgeDir, { recursive: true });
    }

    fs.writeFileSync(
      path.join(badgeDir, "downloads.json"),
      JSON.stringify(badgeJson, null, 2)
    );
  } catch (e) {
    console.error("Script Fehler:", e);
  }
})();
