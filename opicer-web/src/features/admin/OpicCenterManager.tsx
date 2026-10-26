"use client";

import { useEffect, useState } from "react";
import { opicCenterApi } from "./api";
import type { AdminOpicCenter, AdminOpicCenterInput } from "./types";
import { adminUi } from "@/features/admin/ui";
import { parseAdminError } from "@/features/admin/errors";

const REGIONS = [
  "서울", "경기", "인천", "강원", "대전", "세종", "충북", "충남", "대구",
  "경북", "부산", "울산", "경남", "광주", "전북", "전남", "제주",
];

type Form = {
  name: string;
  address: string;
  regionName: string;
  phone: string;
  subwayLines: string;
  directions: string;
  latitude: string;
  longitude: string;
};

const emptyForm: Form = {
  name: "",
  address: "",
  regionName: "서울",
  phone: "",
  subwayLines: "",
  directions: "",
  latitude: "",
  longitude: "",
};

function messageOf(err: unknown): string {
  const { code, message } = parseAdminError(err);
  if (code === "OPIC_CENTER_DUPLICATE") return "같은 이름과 주소의 센터가 이미 있어요.";
  return message;
}

function toInput(form: Form): AdminOpicCenterInput {
  const optional = (value: string) => (value.trim() ? value.trim() : undefined);
  const coordinate = (value: string) => (value.trim() ? Number(value) : undefined);
  return {
    name: form.name.trim(),
    address: form.address.trim(),
    regionName: form.regionName,
    phone: optional(form.phone),
    subwayLines: optional(form.subwayLines),
    directions: optional(form.directions),
    latitude: coordinate(form.latitude),
    longitude: coordinate(form.longitude),
  };
}

/** Admin-entered test centers. Leaving the coordinates empty looks them up from the address. */
export function OpicCenterManager({ onChanged }: { onChanged: () => void }) {
  const [centers, setCenters] = useState<AdminOpicCenter[]>([]);
  const [form, setForm] = useState<Form>(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  // Coordinates as loaded for editing: unchanged ones must not survive an address change.
  const [loadedCoordinates, setLoadedCoordinates] = useState({ latitude: "", longitude: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    opicCenterApi
      .list()
      .then((data) => alive && setCenters(data))
      .catch((err) => alive && setError(messageOf(err)));
    return () => {
      alive = false;
    };
  }, []);

  async function reload() {
    setCenters(await opicCenterApi.list());
    onChanged();
  }

  function startEdit(center: AdminOpicCenter) {
    setEditId(center.id);
    setForm({
      name: center.name,
      address: center.address,
      regionName: center.regionName ?? "서울",
      phone: center.phone ?? "",
      subwayLines: center.subwayLines ?? "",
      directions: center.directions ?? "",
      latitude: center.latitude?.toString() ?? "",
      longitude: center.longitude?.toString() ?? "",
    });
    setLoadedCoordinates({
      latitude: center.latitude?.toString() ?? "",
      longitude: center.longitude?.toString() ?? "",
    });
    setError(null);
    setNotice(null);
  }

  /** A new address needs new coordinates, unless the admin typed coordinates themselves. */
  function changeAddress(address: string) {
    setForm((f) => {
      const untouched =
        Boolean(f.latitude) && f.latitude === loadedCoordinates.latitude && f.longitude === loadedCoordinates.longitude;
      return untouched ? { ...f, address, latitude: "", longitude: "" } : { ...f, address };
    });
  }

  function cancelEdit() {
    setEditId(null);
    setForm(emptyForm);
    setLoadedCoordinates({ latitude: "", longitude: "" });
    setError(null);
  }

  const halfCoordinates = Boolean(form.latitude.trim()) !== Boolean(form.longitude.trim());

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (halfCoordinates) return;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const saved = editId
        ? await opicCenterApi.update(editId, toInput(form))
        : await opicCenterApi.create(toInput(form));
      setNotice(
        saved.geocodeWarning
          ? `저장했어요. 좌표 없음: ${saved.geocodeWarning}`
          : `저장했어요. 좌표 ${saved.latitude?.toFixed(5)}, ${saved.longitude?.toFixed(5)}`
      );
      cancelEdit();
      await reload();
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(center: AdminOpicCenter) {
    setError(null);
    try {
      if (center.active) await opicCenterApi.deactivate(center.id);
      else await opicCenterApi.activate(center.id);
      await reload();
    } catch (err) {
      setError(messageOf(err));
    }
  }

  const field = (key: keyof Form) => ({
    value: form[key],
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [key]: e.target.value })),
  });

  return (
    <div className={adminUi.section}>
      <form onSubmit={handleSubmit} className={adminUi.card}>
        <h3 className={adminUi.cardTitle}>{editId ? "시험센터 수정" : "시험센터 추가"}</h3>
        <p className="text-sm text-muted-foreground">
          좌표를 비워 두면 주소로 자동으로 찾아요(네이버 지오코딩). 찾지 못하면 좌표 없이 저장되고, 직접 입력할 수 있어요.
        </p>
        {error && <p role="alert" className={adminUi.error}>{error}</p>}
        {notice && <p className="text-sm text-primary-strong">{notice}</p>}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>센터 이름</span>
            <input required maxLength={255} className={adminUi.control} placeholder="예: 강남 오픽 센터" {...field("name")} />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>지역</span>
            <select className={adminUi.control} {...field("regionName")}>
              {REGIONS.map((region) => (
                <option key={region} value={region}>{region}</option>
              ))}
            </select>
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>도로명 주소</span>
          <input
            required
            maxLength={500}
            className={adminUi.control}
            placeholder="예: 서울 강남구 테헤란로 123"
            value={form.address}
            onChange={(e) => changeAddress(e.target.value)}
          />
        </label>
        {editId && loadedCoordinates.latitude && !form.latitude && (
          <p className="text-xs text-muted-foreground">주소가 바뀌어 좌표를 비웠어요. 저장하면 새 주소로 다시 찾아요.</p>
        )}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>전화번호</span>
            <input maxLength={255} className={adminUi.control} {...field("phone")} />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>가까운 지하철</span>
            <input maxLength={255} className={adminUi.control} placeholder="예: 2호선 강남역" {...field("subwayLines")} />
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>찾아가는 길</span>
          <textarea rows={3} maxLength={2000} className={adminUi.control} {...field("directions")} />
        </label>
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>위도 (선택)</span>
            <input type="number" step="any" min={-90} max={90} className={adminUi.control} placeholder="37.5" {...field("latitude")} />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>경도 (선택)</span>
            <input type="number" step="any" min={-180} max={180} className={adminUi.control} placeholder="127.03" {...field("longitude")} />
          </label>
        </div>
        {halfCoordinates && <p className={adminUi.error}>위도와 경도는 둘 다 입력하거나 둘 다 비워 두세요.</p>}
        <div className={adminUi.actions}>
          <button type="submit" disabled={saving || halfCoordinates} className={adminUi.primaryButton}>
            {saving ? "저장 중..." : editId ? "수정 저장" : "추가"}
          </button>
          {editId && (
            <button type="button" onClick={cancelEdit} className={adminUi.secondaryButton}>
              취소
            </button>
          )}
        </div>
      </form>

      <div className={adminUi.tableWrap}>
        <table className={adminUi.table}>
          <thead className={adminUi.thead}>
            <tr>
              <th className={adminUi.th}>지역</th>
              <th className={adminUi.th}>센터</th>
              <th className={adminUi.th}>좌표</th>
              <th className={adminUi.th}>상태</th>
              <th className={adminUi.th}>관리</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {centers.length === 0 && (
              <tr>
                <td colSpan={5} className={adminUi.tdEmpty}>등록된 시험센터가 없어요.</td>
              </tr>
            )}
            {centers.map((center) => (
              <tr key={center.id} className={adminUi.tr}>
                <td className={adminUi.tdMuted}>{center.regionName ?? "-"}</td>
                <td className={adminUi.td}>
                  <p className="font-medium">{center.name}</p>
                  <p className="text-xs text-muted-foreground">{center.address}</p>
                </td>
                <td className={adminUi.tdSmall}>
                  {center.latitude != null && center.longitude != null ? (
                    `${center.latitude.toFixed(4)}, ${center.longitude.toFixed(4)}`
                  ) : (
                    <span className="text-destructive">없음 (지도에 안 보임)</span>
                  )}
                </td>
                <td className={adminUi.td}>
                  <span className={center.active ? adminUi.pillOn : adminUi.pillOff}>
                    {center.active ? "공개" : "숨김"}
                  </span>
                </td>
                <td className={adminUi.td}>
                  <div className={adminUi.actions}>
                    <button type="button" onClick={() => startEdit(center)} className={adminUi.linkButton}>
                      수정
                    </button>
                    <button type="button" onClick={() => toggleActive(center)} className={adminUi.linkButton}>
                      {center.active ? "숨기기" : "공개"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
