import json
import csv
import folium
from datetime import datetime
import sys
import os
import re

if len(sys.argv) != 6:
    sys.exit(1)

filePath = sys.argv[1]
start_date_str = sys.argv[2]
end_date_str = sys.argv[3]
checkboxSelected = sys.argv[4]
fileNameInput = sys.argv[5]

# Sanitize file name input to avoid illegal characters
if fileNameInput:
    safe_name = re.sub(r'[^a-zA-Z0-9_-]', '_', fileNameInput)
else:
    safe_name = "filtered_map"


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
csv_rows = []
all_points = []

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
                all_points.append((lat, lon))

                # Marker for each timeline point
                folium.CircleMarker(
                    location=(lat, lon),
                    radius=3,
                    color='green',
                    fill=True,
                    fill_color='green',
                    popup=f"Timeline point: {entry.get('startTime', '')}"
                ).add_to(m)
                csv_rows.append([lat, lon, "timelinePoint", entry.get("startTime", ""), entry.get("endTime", ""), idx])

        if len(path) > 1:
            folium.PolyLine(path, color="green", weight=2, opacity=0.6).add_to(m)
            path_count += 1
        elif len(path) == 1:
            path_count += 1
            # Single timeline point already added as marker above
            pass

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
        # Polyline for the activity path
        folium.PolyLine(path, color="blue", weight=2, opacity=0.6, popup=popup_text).add_to(m)
        path_count += 1

        # Mark start and end points
        for lat_lon, point_type in zip(path, ["activity-start", "activity-end"]):
            folium.CircleMarker(
                location=lat_lon,
                radius=4,
                color='blue',
                fill=True,
                fill_color='blue',
                popup=f"{point_type}: {entry.get('startTime')} → {entry.get('endTime')}"
            ).add_to(m)
            csv_rows.append([lat_lon[0], lat_lon[1], point_type, entry.get("startTime", ""), entry.get("endTime", ""), idx])
            all_points.append(lat_lon)

    # 3. visit
    if "visit" in entry:
        place_location = entry["visit"].get("topCandidate", {}).get("placeLocation")
        if place_location:
            lat, lon = map(float, place_location.replace("geo:", "").split(","))
            folium.CircleMarker(
                location=(lat, lon),
                radius=5,
                color='red',
                fill=True,
                fill_color='red',
                popup=f"Visit: {entry.get('startTime')} → {entry.get('endTime')}"
            ).add_to(m)
            csv_rows.append([lat, lon, "visit", entry.get("startTime", ""), entry.get("endTime", ""), idx])
            all_points.append((lat, lon))

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

# Save directory setup
output_dir = os.path.join(os.getcwd(), "maps")
os.makedirs(output_dir, exist_ok=True)

# Save map

# Automatically zoom to all points
if all_points:
    m.fit_bounds(all_points)

# Add layer control UI
folium.LayerControl(position='topright', collapsed=False).add_to(m)

# Generate unique filenames with timestamp
timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
html_name = f"{safe_name}_{timestamp}.html"
filepath = os.path.join(output_dir, html_name)

if checkboxSelected == "true":
    csv_name = f"{safe_name}_{timestamp}.csv"
    csv_path = os.path.join(output_dir, csv_name)

# Save map
m.save(filepath)
print(f"\n✅ Map created with {path_count} paths. Open '{filepath}' to view.")

# Save CSV if selected
if checkboxSelected == "true":
    with open(csv_path, mode="w", newline="", encoding="utf-8") as f_csv:
        writer = csv.writer(f_csv)
        writer.writerow(["latitude", "longitude", "type", "startTime", "endTime", "entryID"])
        writer.writerows(csv_rows)
    print(f"✅ Exported {len(csv_rows)} coordinates to '{csv_path}'.")
