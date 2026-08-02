#!/usr/bin/env python3
"""Generates the three Tiled JSON maps under assets/maps.

The maps are ordinary .tmj files and can be opened and edited in Tiled once generated;
this script exists so the initial layouts are readable as code rather than as 600-number
arrays, and so they can be regenerated after a layout change.

Usage:  python3 tools/generate_maps.py
"""

import json
import os

TILE = 32

# Local tile ids from assets/maps/meridian_tileset.tsj; map data uses gid = id + 1.
VOID, PLATING, PLATING_WORN, HULL_WALL = 0, 1, 2, 3
REGOLITH, REGOLITH_DARK, SOIL, GRAVEL = 4, 5, 6, 7
GLASS, COOLANT, MOSS, CRYSTAL = 8, 9, 10, 11
HAB_FLOOR, CATWALK, ROCK, SPORE_GRASS = 12, 13, 14, 15

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAPS_DIR = os.path.join(ROOT, "assets", "maps")


class Grid:
    """A y-up tile grid that knows how to emit Tiled's y-down row order."""

    def __init__(self, width, height, fill=0):
        self.width = width
        self.height = height
        self.cells = [[fill] * width for _ in range(height)]

    def set(self, x, y, value):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.cells[y][x] = value

    def rect(self, x1, y1, x2, y2, value):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for x in range(min(x1, x2), max(x1, x2) + 1):
                self.set(x, y, value)

    def border(self, x1, y1, x2, y2, value):
        for x in range(x1, x2 + 1):
            self.set(x, y1, value)
            self.set(x, y2, value)
        for y in range(y1, y2 + 1):
            self.set(x1, y, value)
            self.set(x2, y, value)

    def outline(self, value):
        self.border(0, 0, self.width - 1, self.height - 1, value)

    def to_tiled_data(self, gid_offset=1):
        """Flattens top-down, which is the order Tiled stores tile data in."""
        data = []
        for y in range(self.height - 1, -1, -1):
            for x in range(self.width):
                value = self.cells[y][x]
                data.append(0 if value is None else value + gid_offset)
        return data

    def to_flag_data(self, marker_gid=4):
        """Collision and farmable layers only care about zero vs non-zero."""
        data = []
        for y in range(self.height - 1, -1, -1):
            for x in range(self.width):
                data.append(marker_gid if self.cells[y][x] else 0)
        return data


def tile_layer(layer_id, name, width, height, data):
    return {
        "data": data,
        "height": height,
        "id": layer_id,
        "name": name,
        "opacity": 1,
        "type": "tilelayer",
        "visible": True,
        "width": width,
        "x": 0,
        "y": 0,
    }


def prop(name, value):
    kind = "bool" if isinstance(value, bool) else ("int" if isinstance(value, int) else "string")
    return {"name": name, "type": kind, "value": value}


def obj(object_id, name, kind, x, y, height, properties=None):
    """Places an object on tile (x, y) using Tiled's y-down pixel coordinates."""
    entry = {
        "class": kind,
        "height": TILE,
        "id": object_id,
        "name": name,
        "rotation": 0,
        "visible": True,
        "width": TILE,
        "x": x * TILE,
        "y": (height - 1 - y) * TILE,
    }
    if properties:
        entry["properties"] = properties
    return entry


def write_map(filename, width, height, display_name, ambient, ground, overlay, collision, farmable, objects):
    document = {
        "compressionlevel": -1,
        "height": height,
        "infinite": False,
        "layers": [
            tile_layer(1, "ground", width, height, ground.to_tiled_data()),
            tile_layer(2, "overlay", width, height, overlay.to_tiled_data(1) if overlay else [0] * (width * height)),
            tile_layer(3, "collision", width, height, collision.to_flag_data()),
            tile_layer(4, "farmable", width, height, farmable.to_flag_data(7)),
            {
                "draworder": "topdown",
                "id": 5,
                "name": "objects",
                "objects": objects,
                "opacity": 1,
                "type": "objectgroup",
                "visible": True,
                "x": 0,
                "y": 0,
            },
        ],
        "nextlayerid": 6,
        "nextobjectid": len(objects) + 1,
        "orientation": "orthogonal",
        "properties": [prop("displayName", display_name), prop("ambientColour", ambient)],
        "renderorder": "right-down",
        "tiledversion": "1.11.0",
        "tileheight": TILE,
        "tilesets": [{"firstgid": 1, "source": "meridian_tileset.tsj"}],
        "tilewidth": TILE,
        "type": "map",
        "version": "1.10",
        "width": width,
    }
    path = os.path.join(MAPS_DIR, filename)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(document, handle, indent=1)
        handle.write("\n")
    print("wrote", os.path.relpath(path, ROOT))


def overlay_grid(width, height):
    grid = Grid(width, height, None)
    return grid


def build_colony():
    w, h = 30, 20
    ground = Grid(w, h, REGOLITH)
    overlay = overlay_grid(w, h)
    solid = Grid(w, h, 0)
    farm = Grid(w, h, 0)

    # Station apron and the gravel path out to the landing pad.
    ground.rect(3, 5, 21, 16, PLATING)
    ground.rect(22, 9, 28, 11, GRAVEL)
    ground.rect(23, 9, 26, 11, PLATING_WORN)
    ground.rect(1, 17, 28, 18, REGOLITH_DARK)

    # Habitat block: solid shell with a door gap at (7, 12).
    ground.rect(5, 13, 9, 16, HAB_FLOOR)
    solid.border(4, 12, 10, 17, 1)
    solid.set(7, 12, 0)
    ground.set(7, 12, HAB_FLOOR)

    # Utility pads for the modules and the bench.
    for pad in [(13, 7), (17, 7), (13, 10), (17, 10)]:
        ground.rect(pad[0] - 1, pad[1] - 1, pad[0] + 1, pad[1] + 1, CATWALK)
        solid.set(pad[0], pad[1], 1)

    # A couple of collapsed hull sections to walk around.
    ground.rect(19, 15, 20, 16, PLATING_WORN)
    solid.rect(11, 15, 12, 16, 1)
    ground.rect(11, 15, 12, 16, HULL_WALL)

    ground.outline(VOID)
    solid.outline(1)

    objects = [
        obj(1, "landing_pad", "spawn", 24, 10, h),
        obj(2, "from_terrace", "spawn", 26, 10, h),
        obj(3, "to_terrace", "portal", 28, 10, h,
            [prop("target", "terrace"), prop("spawn", "from_colony")]),
        obj(4, "hab_bunk", "bed", 6, 15, h, [prop("title", "your bunk")]),
        obj(5, "module_oxygen", "module", 13, 7, h, [prop("module", "oxygen_recycler")]),
        obj(6, "module_comms", "module", 17, 7, h, [prop("module", "comms_array")]),
        obj(7, "module_hydro", "module", 13, 10, h, [prop("module", "hydroponics_controller")]),
        obj(8, "fabricator", "crafting_station", 17, 10, h,
            [prop("station", "fabricator"), prop("title", "fabrication bench")]),
        obj(9, "salvage_a", "resource_node", 5, 8, h,
            [prop("item", "scrap_alloy"), prop("min", 2), prop("max", 3), prop("respawnDays", 3)]),
        obj(10, "salvage_b", "resource_node", 8, 9, h,
            [prop("item", "scrap_alloy"), prop("min", 1), prop("max", 3), prop("respawnDays", 3)]),
        obj(11, "salvage_c", "resource_node", 20, 14, h,
            [prop("item", "scrap_alloy"), prop("min", 2), prop("max", 4), prop("respawnDays", 3)]),
        obj(12, "salvage_d", "resource_node", 20, 6, h,
            [prop("item", "salvaged_cell"), prop("min", 1), prop("max", 2), prop("respawnDays", 4)]),
        obj(13, "salvage_e", "resource_node", 6, 6, h,
            [prop("item", "salvaged_cell"), prop("min", 1), prop("max", 1), prop("respawnDays", 5)]),
        obj(14, "log_intake", "crew_log", 19, 10, h, [prop("log", "log_intake")]),
        obj(15, "meridian_station", "landmark", 11, 3, h, [prop("title", "Meridian Station")]),
        obj(16, "notice_board", "sign", 22, 12, h,
            [prop("text", "MERIDIAN STATION - crew of eleven - atmospheric processing since year 1")]),
    ]

    write_map("colony.tmj", w, h, "Meridian Station", "121821", ground, overlay, solid, farm, objects)


def build_terrace():
    w, h = 26, 18
    ground = Grid(w, h, REGOLITH)
    overlay = overlay_grid(w, h)
    solid = Grid(w, h, 0)
    farm = Grid(w, h, 0)

    ground.rect(4, 3, 18, 13, SOIL)
    farm.rect(5, 4, 17, 12, 1)
    ground.rect(1, 8, 4, 10, GRAVEL)
    ground.rect(12, 14, 14, 16, GRAVEL)
    ground.rect(19, 2, 24, 16, REGOLITH_DARK)

    # Irrigation trench: pretty, and something to path around.
    ground.rect(10, 13, 16, 13, COOLANT)
    solid.rect(10, 13, 16, 13, 1)
    solid.set(13, 13, 0)
    ground.set(13, 13, GRAVEL)

    solid.rect(21, 9, 22, 10, 1)
    ground.rect(21, 9, 22, 10, ROCK)

    ground.outline(VOID)
    solid.outline(1)

    objects = [
        obj(1, "from_colony", "spawn", 3, 9, h),
        obj(2, "to_colony", "portal", 1, 9, h,
            [prop("target", "colony"), prop("spawn", "from_terrace")]),
        obj(3, "from_biome", "spawn", 13, 14, h),
        obj(4, "to_biome", "portal", 13, 16, h,
            [prop("target", "biome"), prop("spawn", "from_terrace")]),
        obj(5, "south_terrace", "landmark", 10, 8, h, [prop("title", "South Terrace")]),
        obj(6, "log_maintenance", "crew_log", 20, 6, h, [prop("log", "log_maintenance")]),
        obj(7, "stalks_a", "resource_node", 20, 12, h,
            [prop("item", "resin_fibre"), prop("min", 2), prop("max", 3), prop("respawnDays", 2)]),
        obj(8, "stalks_b", "resource_node", 22, 14, h,
            [prop("item", "resin_fibre"), prop("min", 1), prop("max", 3), prop("respawnDays", 2)]),
        obj(9, "stalks_c", "resource_node", 19, 3, h,
            [prop("item", "resin_fibre"), prop("min", 2), prop("max", 2), prop("respawnDays", 2)]),
        obj(10, "glass_a", "resource_node", 23, 8, h,
            [prop("item", "silica_shard"), prop("min", 1), prop("max", 2), prop("respawnDays", 3)]),
        obj(11, "glass_b", "resource_node", 23, 5, h,
            [prop("item", "silica_shard"), prop("min", 2), prop("max", 3), prop("respawnDays", 3)]),
        obj(12, "trench_sign", "sign", 9, 14, h,
            [prop("text", "IRRIGATION LOOP 2 - programme loaded: 36500 days remaining")]),
    ]

    write_map("terrace.tmj", w, h, "South Terrace", "141c18", ground, overlay, solid, farm, objects)


def build_biome():
    w, h = 28, 20
    ground = Grid(w, h, MOSS)
    overlay = overlay_grid(w, h)
    solid = Grid(w, h, 0)
    farm = Grid(w, h, 0)

    ground.rect(9, 6, 18, 13, CRYSTAL)
    ground.rect(11, 8, 16, 11, GLASS)
    ground.rect(12, 14, 16, 18, SPORE_GRASS)
    ground.rect(2, 2, 7, 8, SPORE_GRASS)
    ground.rect(20, 14, 26, 18, SPORE_GRASS)

    for x1, y1, x2, y2 in [(4, 10, 6, 12), (8, 16, 10, 17), (19, 3, 21, 5),
                           (23, 9, 25, 11), (16, 16, 17, 17), (2, 14, 3, 15)]:
        ground.rect(x1, y1, x2, y2, ROCK)
        solid.rect(x1, y1, x2, y2, 1)

    ground.outline(VOID)
    solid.outline(1)

    objects = [
        obj(1, "from_terrace", "spawn", 14, 16, h),
        obj(2, "to_terrace", "portal", 14, 18, h,
            [prop("target", "terrace"), prop("spawn", "from_biome")]),
        obj(3, "glass_hollow", "landmark", 13, 9, h,
            [prop("title", "The Glass Hollow"), prop("nightHazard", True)]),
        obj(4, "log_dreams", "crew_log", 5, 12, h, [prop("log", "log_dreams")]),
        obj(5, "log_vote", "crew_log", 22, 7, h, [prop("log", "log_vote")]),
        obj(6, "log_last", "crew_log", 13, 4, h, [prop("log", "log_last")]),
        obj(7, "shard_a", "resource_node", 7, 7, h,
            [prop("item", "silica_shard"), prop("min", 2), prop("max", 4), prop("respawnDays", 3)]),
        obj(8, "shard_b", "resource_node", 9, 14, h,
            [prop("item", "silica_shard"), prop("min", 1), prop("max", 3), prop("respawnDays", 3)]),
        obj(9, "shard_c", "resource_node", 20, 12, h,
            [prop("item", "silica_shard"), prop("min", 2), prop("max", 3), prop("respawnDays", 3)]),
        obj(10, "shard_d", "resource_node", 24, 5, h,
            [prop("item", "silica_shard"), prop("min", 1), prop("max", 4), prop("respawnDays", 3)]),
        obj(11, "lumen_a", "resource_node", 11, 6, h,
            [prop("item", "lumen_shard"), prop("min", 1), prop("max", 2), prop("respawnDays", 4),
             prop("tool", "cutter")]),
        obj(12, "lumen_b", "resource_node", 18, 15, h,
            [prop("item", "lumen_shard"), prop("min", 1), prop("max", 2), prop("respawnDays", 4),
             prop("tool", "cutter")]),
        obj(13, "fibre_a", "resource_node", 6, 4, h,
            [prop("item", "resin_fibre"), prop("min", 2), prop("max", 3), prop("respawnDays", 2)]),
        obj(14, "fibre_b", "resource_node", 24, 16, h,
            [prop("item", "resin_fibre"), prop("min", 2), prop("max", 4), prop("respawnDays", 2)]),
        obj(15, "hollow_marker", "sign", 13, 12, h,
            [prop("text", "Eleven pairs of boots, set down in a row. Nobody carried them back.")]),
    ]

    write_map("biome.tmj", w, h, "The Glass Hollow", "0d1a16", ground, overlay, solid, farm, objects)


if __name__ == "__main__":
    os.makedirs(MAPS_DIR, exist_ok=True)
    build_colony()
    build_terrace()
    build_biome()
