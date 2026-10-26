"use client";

import { useEffect, useRef, useState } from "react";
import {
  NAVER_MAP_CLIENT_ID,
  loadNaverMaps,
  type NaverInfoWindow,
  type NaverMap,
  type NaverMaps,
  type NaverMarker,
} from "@/features/guide/naver-maps";
import type { OpicCenter } from "@/features/opic/types";

type Props = {
  centers: OpicCenter[];
  selectedId: string | null;
  onSelect: (centerId: string) => void;
  /** Defaults to NEXT_PUBLIC_NAVER_MAP_CLIENT_ID. */
  clientId?: string;
};

/** Seoul City Hall, used until the centers' bounds are known. */
const DEFAULT_CENTER = { lat: 37.5666, lng: 126.9784 };
const SINGLE_CENTER_ZOOM = 15;

function located(center: OpicCenter): center is OpicCenter & { latitude: number; longitude: number } {
  return center.latitude != null && center.longitude != null;
}

/**
 * Test centers on a NAVER map. Renders nothing without a client id, and a short note if the script or the key
 * fails; the list next to it works either way.
 */
export function OpicCenterMap({ centers, selectedId, onSelect, clientId = NAVER_MAP_CLIENT_ID }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<NaverMap | null>(null);
  const markersRef = useRef(new Map<string, NaverMarker>());
  const infoRef = useRef<NaverInfoWindow | null>(null);
  const onSelectRef = useRef(onSelect);
  const [maps, setMaps] = useState<NaverMaps | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    onSelectRef.current = onSelect;
  });

  useEffect(() => {
    if (!clientId) return;
    let alive = true;
    window.navermap_authFailure = () => alive && setFailed(true);
    loadNaverMaps(clientId)
      .then((loaded) => alive && setMaps(loaded))
      .catch(() => alive && setFailed(true));
    return () => {
      alive = false;
    };
  }, [clientId]);

  // Create the map once the script is ready.
  useEffect(() => {
    if (!maps || !containerRef.current || mapRef.current) return;
    mapRef.current = new maps.Map(containerRef.current, {
      center: new maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
      zoom: 11,
      zoomControl: true,
    });
  }, [maps]);

  // One marker per visible, located center; fit the view to them.
  useEffect(() => {
    const map = mapRef.current;
    if (!maps || !map) return;
    const markers = markersRef.current;
    const listeners: unknown[] = [];
    const shown = centers.filter(located);
    for (const center of shown) {
      const marker = new maps.Marker({
        position: new maps.LatLng(center.latitude, center.longitude),
        map,
        title: center.name,
      });
      listeners.push(maps.Event.addListener(marker, "click", () => onSelectRef.current(center.id)));
      markers.set(center.id, marker);
    }
    if (shown.length === 1) {
      map.setCenter(new maps.LatLng(shown[0].latitude, shown[0].longitude));
      map.setZoom(SINGLE_CENTER_ZOOM);
    } else if (shown.length > 1) {
      const lats = shown.map((c) => c.latitude);
      const lngs = shown.map((c) => c.longitude);
      map.fitBounds(
        new maps.LatLngBounds(
          new maps.LatLng(Math.min(...lats), Math.min(...lngs)),
          new maps.LatLng(Math.max(...lats), Math.max(...lngs))
        ),
        { top: 40, right: 40, bottom: 40, left: 40 }
      );
    }
    return () => {
      listeners.forEach((listener) => maps.Event.removeListener(listener));
      markers.forEach((marker) => marker.setMap(null));
      markers.clear();
      infoRef.current?.close();
    };
  }, [maps, centers]);

  // Follow the selection from the list or a marker.
  useEffect(() => {
    const map = mapRef.current;
    const marker = selectedId ? markersRef.current.get(selectedId) : undefined;
    if (!maps || !map || !marker) return;
    const center = centers.find((c) => c.id === selectedId);
    map.panTo(marker.getPosition());
    infoRef.current?.close();
    // Text only, never HTML: names come from admins but are still data.
    const label = document.createElement("div");
    label.className = "px-2 py-1 text-xs font-semibold text-neutral-900";
    label.textContent = center?.name ?? "";
    infoRef.current = new maps.InfoWindow({ content: label, borderWidth: 1 });
    infoRef.current.open(map, marker);
  }, [maps, centers, selectedId]);

  if (!clientId) return null;
  if (failed) {
    return (
      <p className="rounded-2xl bg-secondary px-4 py-3 text-sm text-muted-foreground">
        지도를 불러오지 못했어요. 아래 목록에서 센터를 확인하세요.
      </p>
    );
  }

  const unlocated = centers.length - centers.filter(located).length;
  return (
    <div className="flex flex-col gap-1.5">
      <div
        ref={containerRef}
        role="region"
        aria-label="시험센터 지도"
        className="h-64 w-full overflow-hidden rounded-2xl border bg-secondary sm:h-80"
      />
      {unlocated > 0 && (
        <p className="text-xs text-muted-foreground">위치 정보가 없는 센터 {unlocated}곳은 지도에 표시되지 않아요.</p>
      )}
    </div>
  );
}
