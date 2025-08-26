import json
import csv
import folium
from datetime import datetime
import sys

if len(sys.argv) != 6:
    sys.exit(1)

filePath = sys.argv[1]
start_date_str = sys.argv[2]
end_date_str = sys.argv[3]
checkboxSelected = sys.argv[4]
fileNameInput = sys.argv[5]


def parse_datetime(dt_string):
    try:
        return datetime.fromisoformat(dt_string.replace("Z", "+00:00")).astimezone().replace(tzinfo=None)
    except Exception:
        return None

# Input from user
start_date = datetime.strptime(start_date_str, "%Y-%m-%d")
end_date = datetime.strptime(end_date_str, "%Y-%m-%d")

# Load JSON
with open(filePath, "r", encoding="utf-8") as f:
    data = json.load(f)

# Create map
m = folium.Map(location=[0, 0], zoom_start=2, control_scale=True, tiles=None)

# Add base map layers for toggling
folium.TileLayer("OpenStreetMap", name="OpenStreetMap").add_to(m)
folium.TileLayer("CartoDB dark_matter", name="Dark Mode").add_to(m)
folium.TileLayer("Cartodb Positron", name="Positron").add_to(m)
folium.TileLayer(
    tiles="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
    attr="Esri",
    name="Satellite",
    overlay=False,
    control=True
).add_to(m)

path_count = 0
first_path_set = False
csv_rows = []

# Define emoji icons for motion types
type_icon = {
    "walking": "🚶",
    "running": "🏃",
    "in passenger vehicle": "🚗",
    "in vehicle": "🚘",
    "in bus": "🚌",
    "in train": "🚆",
    "in subway": "🚇",
    "in airplane": "✈️",
    "in ferry": "⛴️",
    "on bicycle": "🚲",
    "flying": "🛫",
    "still": "⏸️",
    "unknown": "❓"
}

# Loop through all entries
for idx, entry in enumerate(data):
    entry_start = parse_datetime(entry.get("startTime", ""))
    if not entry_start or not (start_date <= entry_start <= end_date):
        continue

    # 1. timelinePath
    if "timelinePath" in entry:
        path = []
        for point in entry["timelinePath"]:
            if "point" in point:
                lat, lon = map(float, point["point"].replace("geo:", "").split(","))
                path.append((lat, lon))
                csv_rows.append([lat, lon, "timelinePoint", entry.get("startTime", ""), entry.get("endTime", ""), idx])
        
        if len(path) > 1:
            folium.PolyLine(path, color="green", weight=2, opacity=0.6).add_to(m)
            if not first_path_set:
                m.location = path[0]
                first_path_set = True
            path_count += 1

        elif len(path) == 1:
            lat, lon = path[0]
            folium.CircleMarker(
                location=(lat, lon),
                radius=3,
                color='gray',
                fill=True,
                fill_color='gray',
                popup=f"Single timeline point: {entry.get('startTime', '')}"
            ).add_to(m)
            csv_rows.append([lat, lon, "timelinePoint-single", entry.get("startTime", ""), entry.get("endTime", ""), idx])
            

    # 2. activity with popup
    elif "activity" in entry and "start" in entry["activity"] and "end" in entry["activity"]:
        start = entry["activity"]["start"].replace("geo:", "").split(",")
        end = entry["activity"]["end"].replace("geo:", "").split(",")
        path = [(float(start[0]), float(start[1])), (float(end[0]), float(end[1]))]

        activity_type = entry["activity"].get("topCandidate", {}).get("type", "unknown")
        emoji = type_icon.get(activity_type, "➡️")
        distance = entry["activity"].get("distanceMeters", "unknown")
        popup_text = f"""
        <b>Activity</b><br>
        Type: {emoji} {activity_type}<br>
        Distance: {distance} meters<br>
        From: {entry.get('startTime')}<br>
        To: {entry.get('endTime')}
        """
        folium.PolyLine(path, color="blue", weight=2, opacity=0.6, popup=popup_text).add_to(m)
        if not first_path_set:
            m.location = path[0]
            first_path_set = True
        path_count += 1
        csv_rows.append([float(start[0]), float(start[1]), "activity-start", entry.get("startTime", ""), entry.get("endTime", ""), idx])
        csv_rows.append([float(end[0]), float(end[1]), "activity-end", entry.get("startTime", ""), entry.get("endTime", ""), idx])

    # 3. visit
    if "visit" in entry:
        place_location = entry["visit"].get("topCandidate", {}).get("placeLocation")
        if place_location:
            lat, lon = map(float, place_location.replace("geo:", "").split(","))
            folium.CircleMarker(
                location=(lat, lon),
                radius=4,
                color='red',
                fill=True,
                fill_color='red',
                popup=f"Visit: {entry.get('startTime')} → {entry.get('endTime')}"
            ).add_to(m)
            csv_rows.append([lat, lon, "visit", entry.get("startTime", ""), entry.get("endTime", ""), idx])

# Add legend
legend_html = """
<div style="position: fixed; bottom: 50px; left: 50px; width: 200px; height: 100px;
     background-color: white; z-index:9999; font-size:14px; border:2px solid grey; padding: 10px;">
<b>Legend</b><br>
<span style="color:green;">&#9679;</span> Timeline Path<br>
<span style="color:blue;">&#9679;</span> Activity Segment<br>
<span style="color:red;">&#9679;</span> Visit Location
</div>
"""
m.get_root().html.add_child(folium.Element(legend_html))

# Write CSV
if checkboxSelected == "true":
    with open("coordinates_export.csv", mode="w", newline="", encoding="utf-8") as f_csv:
        writer = csv.writer(f_csv)
        writer.writerow(["latitude", "longitude", "type", "startTime", "endTime", "entryID"])
        writer.writerows(csv_rows)

# Save map

# Add layer control UI
folium.LayerControl(position='topright', collapsed=False).add_to(m)

if (fileNameInput == ""):
    m.save("filtered_map.html")
else:
    m.save(fileNameInput + ".html")
print(f"\n✅ Map created with {path_count} paths. Open 'filtered_timeline_map.html' to view.")
print(f"✅ Exported {len(csv_rows)} coordinates to 'coordinates_export.csv'.")
