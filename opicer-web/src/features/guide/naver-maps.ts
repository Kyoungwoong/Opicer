/**
 * Minimal typing and a one-time loader for the NAVER Maps JavaScript API v3. Only the members this app uses are
 * declared. The script loads only when a client id is configured.
 */

export type NaverLatLng = { lat(): number; lng(): number };

export type NaverMap = {
  setCenter(position: NaverLatLng): void;
  setZoom(zoom: number): void;
  panTo(position: NaverLatLng): void;
  fitBounds(bounds: NaverLatLngBounds, margin?: { top: number; right: number; bottom: number; left: number }): void;
};

export type NaverMarker = {
  setMap(map: NaverMap | null): void;
  getPosition(): NaverLatLng;
};

export type NaverInfoWindow = {
  open(map: NaverMap, anchor: NaverMarker): void;
  close(): void;
};

export type NaverLatLngBounds = { extend(position: NaverLatLng): void };

export type NaverMaps = {
  LatLng: new (lat: number, lng: number) => NaverLatLng;
  LatLngBounds: new (sw: NaverLatLng, ne: NaverLatLng) => NaverLatLngBounds;
  Map: new (element: HTMLElement, options: { center: NaverLatLng; zoom: number; zoomControl?: boolean }) => NaverMap;
  Marker: new (options: { position: NaverLatLng; map: NaverMap; title?: string }) => NaverMarker;
  InfoWindow: new (options: { content: HTMLElement; borderWidth?: number; disableAnchor?: boolean }) => NaverInfoWindow;
  Event: {
    addListener(target: NaverMarker, event: "click", listener: () => void): unknown;
    removeListener(listener: unknown): void;
  };
};

declare global {
  interface Window {
    naver?: { maps?: NaverMaps };
    /** Called by the NAVER script when the client id is rejected (wrong key or unregistered domain). */
    navermap_authFailure?: () => void;
  }
}

export const NAVER_MAP_CLIENT_ID = process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID ?? "";

const SCRIPT_URL = "https://oapi.map.naver.com/openapi/v3/maps.js";

let loading: Promise<NaverMaps> | null = null;

/** Loads the script once per page; a failed load can be retried by calling again. */
export function loadNaverMaps(clientId: string): Promise<NaverMaps> {
  if (window.naver?.maps) return Promise.resolve(window.naver.maps);
  if (!loading) {
    loading = new Promise<NaverMaps>((resolve, reject) => {
      const script = document.createElement("script");
      script.src = `${SCRIPT_URL}?ncpKeyId=${encodeURIComponent(clientId)}`;
      script.async = true;
      script.onload = () => {
        if (window.naver?.maps) resolve(window.naver.maps);
        else reject(new Error("NAVER Maps did not initialise"));
      };
      script.onerror = () => {
        script.remove();
        loading = null;
        reject(new Error("NAVER Maps script failed to load"));
      };
      document.head.appendChild(script);
    });
  }
  return loading;
}
