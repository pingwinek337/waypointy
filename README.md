# Waypoint Manager (Fabric 1.21.4)

Prosty mod kliencki do Minecrafta 1.21.4 (Fabric) z managerem waypointów.

## Funkcje
- **Manager waypointów** (domyślnie klawisz `B`): lista waypointów z przyciskiem **+ Dodaj**, edycją, usuwaniem i włączaniem/wyłączaniem każdego waypointa.
- **Dodawanie / edycja**: nazwa, współrzędne **X Y Z**, kolor (pole `#RRGGBB` + paleta 16 kolorów), wymiar (Świat / Nether / End). Przycisk **Moja pozycja** wpisuje Twoje aktualne koordynaty.
- **Odległość do waypointa**:
  - znacznik w świecie (romb w kolorze waypointa + nazwa + odległość w m / km),
  - gdy waypoint jest poza ekranem lub za plecami, znacznik przykleja się do krawędzi ekranu w jego kierunku,
  - lista najbliższych waypointów z odległością w lewym górnym rogu (do 8 sztuk).
- Waypointy zapisują się **osobno dla każdego świata / serwera** w `config/waypointmanager/*.json`.
- Pokazują się tylko w wymiarze, w którym zostały utworzone.

## Klawisze (do zmiany w Opcje → Sterowanie)
| Akcja | Domyślnie |
|---|---|
| Otwórz manager waypointów | `B` |
| Dodaj waypoint na mojej pozycji (od razu otwiera okno z wpisanymi XYZ) | `N` |
| Włącz/wyłącz znaczniki | nieprzypisany |

## Budowanie
Wymagane: **JDK 21**.

```
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```
Gotowy mod: `build/libs/waypointmanager-1.0.0.jar` (NIE ten z `-sources`).

Test w grze z poziomu projektu: `./gradlew runClient`.

## Instalacja
1. Fabric Loader 0.16+ dla Minecrafta 1.21.4.
2. Fabric API dla 1.21.4.
3. Wrzuć `waypointmanager-1.0.0.jar` do folderu `mods`.

## Uwagi
- W `gradle.properties` jest `fabric_version=0.111.0+1.21.4`. Jeśli chcesz nowszego Fabric API dla 1.21.4, podmień tę wartość.
- Znaczniki w świecie są rzutowane z pozycji i obrotu kamery bez mixinów. Przy zmianach FOV od sprintu/efektów pozycja znacznika może minimalnie odbiegać.
