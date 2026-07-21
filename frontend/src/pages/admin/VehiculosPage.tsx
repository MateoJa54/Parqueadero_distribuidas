import { useMemo, useState } from 'react';
import { vehiculosApi } from '@/api/vehiculos';
import type { Clasificacion, TipoMoto, TipoVehiculoApi, Vehiculo } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Select } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge, Badge } from '@/ui/Badge';
import { EmptyState, AsyncView, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';
import { rgx } from '@/lib/format';

const TIPOS: TipoVehiculoApi[] = ['Auto', 'Motocicleta', 'Camioneta'];
const CLASIFICACIONES: Clasificacion[] = ['Eléctrico', 'Híbrido', 'Gasolina', 'Diésel'];
const TIPOS_MOTO: TipoMoto[] = ['Deportiva', 'Scooter', 'Motocross'];

type Datos = Record<string, unknown>;

const BASE: Datos = {
  placa: '',
  marca: '',
  modelo: '',
  color: '',
  anio: new Date().getFullYear(),
  clasificacion: 'Gasolina' as Clasificacion,
};

function datosPorTipo(tipo: TipoVehiculoApi): Datos {
  if (tipo === 'Auto') return { ...BASE, numeroPuertas: 4, capacidadMaletero: 400 };
  if (tipo === 'Motocicleta') return { ...BASE, cilindraje: 150, tipoMoto: 'Deportiva' as TipoMoto };
  return { ...BASE, cabina: 4, capacidadCarga: '2.5t' };
}

export function VehiculosPage() {
  const toast = useToast();
  const [incluirInactivos, setIncluirInactivos] = useState(false);
  const { data, loading, error, reload } = useAsync(
    () => vehiculosApi.list(incluirInactivos),
    [incluirInactivos],
  );
  const [q, setQ] = useState('');
  const [modal, setModal] = useState<{ open: boolean; edit?: Vehiculo }>({ open: false });
  const [tipo, setTipo] = useState<TipoVehiculoApi>('Auto');
  const [datos, setDatos] = useState<Datos>(datosPorTipo('Auto'));
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const lista = useMemo(() => {
    const arr = data ?? [];
    const term = q.trim().toLowerCase();
    if (!term) return arr;
    return arr.filter(
      (v) =>
        v.placa.toLowerCase().includes(term) ||
        v.marca.toLowerCase().includes(term) ||
        v.modelo.toLowerCase().includes(term),
    );
  }, [data, q]);

  const abrirNuevo = () => {
    setTipo('Auto');
    setDatos(datosPorTipo('Auto'));
    setErrs({});
    setModal({ open: true });
  };
  const abrirEditar = (v: Vehiculo) => {
    const t = (v.tipo as TipoVehiculoApi) ?? 'Auto';
    setTipo(t);
    setDatos({
      placa: v.placa,
      marca: v.marca,
      modelo: v.modelo,
      color: v.color ?? '',
      anio: v.anio ?? new Date().getFullYear(),
      clasificacion: v.clasificacion ?? 'Gasolina',
      ...(t === 'Auto' && { numeroPuertas: v.numeroPuertas ?? 4, capacidadMaletero: v.capacidadMaletero ?? 400 }),
      ...(t === 'Motocicleta' && { cilindraje: v.cilindraje ?? 150, tipoMoto: v.tipoMoto ?? 'Deportiva' }),
      ...(t === 'Camioneta' && { cabina: v.cabina ?? 4, capacidadCarga: v.capacidadCarga ?? '2.5t' }),
    });
    setErrs({});
    setModal({ open: true, edit: v });
  };

  const cambiarTipo = (t: TipoVehiculoApi) => {
    setTipo(t);
    setDatos((prev) => ({ ...datosPorTipo(t), placa: prev.placa, marca: prev.marca, modelo: prev.modelo, color: prev.color, anio: prev.anio, clasificacion: prev.clasificacion }));
  };

  const set = (k: string, v: unknown) => setDatos((d) => ({ ...d, [k]: v }));

  const validar = () => {
    const e: Record<string, string> = {};
    const placa = String(datos.placa ?? '').toUpperCase();
    const rgxPlaca = tipo === 'Motocicleta' ? rgx.placaMoto : rgx.placaAuto;
    if (!rgxPlaca.test(placa))
      e.placa = tipo === 'Motocicleta' ? 'Formato AB-123C.' : 'Formato ABC-1234.';
    if (!String(datos.marca ?? '').trim()) e.marca = 'Obligatorio.';
    if (!String(datos.modelo ?? '').trim()) e.modelo = 'Obligatorio.';
    const anio = Number(datos.anio);
    if (anio < 1950 || anio > new Date().getFullYear() + 1) e.anio = 'Año inválido.';
    if (tipo === 'Auto') {
      if (Number(datos.numeroPuertas) < 2 || Number(datos.numeroPuertas) > 5)
        e.numeroPuertas = 'Entre 2 y 5.';
      if (Number(datos.capacidadMaletero) < 50 || Number(datos.capacidadMaletero) > 1500)
        e.capacidadMaletero = 'Entre 50 y 1500 L.';
    }
    if (tipo === 'Motocicleta') {
      if (Number(datos.cilindraje) < 50 || Number(datos.cilindraje) > 2500)
        e.cilindraje = 'Entre 50 y 2500 cc.';
    }
    setErrs(e);
    return Object.keys(e).length === 0;
  };

  const guardar = async () => {
    if (!validar()) return;
    setSaving(true);
    try {
      const payload = { ...datos, placa: String(datos.placa).toUpperCase(), anio: Number(datos.anio) };
      if (modal.edit) {
        // En edición no se cambia la placa/tipo; se envían los datos actualizables.
        const { placa: _placa, ...editable } = payload;
        await vehiculosApi.update(modal.edit.id, editable);
        toast.success('Vehículo actualizado');
      } else {
        await vehiculosApi.create({ tipo, datos: payload });
        toast.success('Vehículo creado');
      }
      setModal({ open: false });
      reload();
    } catch (err) {
      toast.error('No se pudo guardar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggleActivo = async (v: Vehiculo) => {
    try {
      if (v.activo) await vehiculosApi.desactivar(v.id);
      else await vehiculosApi.activar(v.id);
      toast.success(v.activo ? 'Vehículo desactivado' : 'Vehículo activado');
      reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  return (
    <>
      <PageHead
        title="Vehículos"
        subtitle="Catálogo de vehículos registrados."
        actions={
          <>
            <label className="row" style={{ gap: 6, fontSize: '0.8125rem' }}>
              <input
                type="checkbox"
                checked={incluirInactivos}
                onChange={(e) => setIncluirInactivos(e.target.checked)}
              />
              Incluir inactivos
            </label>
            <input
              className="input"
              style={{ width: 200 }}
              placeholder="Buscar placa, marca…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              aria-label="Buscar vehículos"
            />
            <Button onClick={abrirNuevo} icon="＋">
              Nuevo vehículo
            </Button>
          </>
        }
      />

      <AsyncView
        loading={loading}
        error={error}
        isEmpty={lista.length === 0}
        onRetry={reload}
        loadingNode={
          <div className="card card-pad">
            <TableSkeleton cols={5} />
          </div>
        }
        emptyNode={<EmptyState title="Sin vehículos" message="Registra el primer vehículo." />}
      >
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Placa</th>
                <th>Vehículo</th>
                <th>Tipo</th>
                <th>Clasificación</th>
                <th>Estado</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((v) => (
                <tr key={v.id}>
                  <td>
                    <strong className="tnum">{v.placa}</strong>
                  </td>
                  <td>
                    {v.marca} {v.modelo}
                    <div className="subtle" style={{ fontSize: '0.75rem' }}>
                      {v.color ?? ''} {v.anio ? `· ${v.anio}` : ''}
                    </div>
                  </td>
                  <td>
                    <Badge tone="info">{v.tipo}</Badge>
                  </td>
                  <td>{v.clasificacion ?? '—'}</td>
                  <td>
                    <ActivoBadge activo={v.activo} />
                  </td>
                  <td>
                    <div className="actions">
                      <Button size="sm" variant="secondary" onClick={() => abrirEditar(v)}>
                        Editar
                      </Button>
                      <Button
                        size="sm"
                        variant={v.activo ? 'ghost' : 'secondary'}
                        onClick={() => toggleActivo(v)}
                      >
                        {v.activo ? 'Desactivar' : 'Activar'}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncView>

      <Modal
        open={modal.open}
        title={modal.edit ? `Editar ${modal.edit.placa}` : 'Nuevo vehículo'}
        onClose={() => setModal({ open: false })}
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal({ open: false })}>
              Cancelar
            </Button>
            <Button onClick={guardar} loading={saving}>
              Guardar
            </Button>
          </>
        }
      >
        <div className="stack">
          {!modal.edit && (
            <Select
              label="Tipo de vehículo"
              value={tipo}
              onChange={(e) => cambiarTipo(e.target.value as TipoVehiculoApi)}
              options={TIPOS.map((t) => ({ value: t, label: t }))}
            />
          )}
          <div className="grid grid-2">
            {(() => {
              const placaHint = tipo === 'Motocicleta' ? 'AB-123C' : 'ABC-1234';
              return (
            <Input
              label="Placa"
              value={String(datos.placa ?? '')}
              onChange={(e) => set('placa', e.target.value.toUpperCase())}
              error={errs.placa}
              disabled={!!modal.edit}
              hint={!modal.edit ? placaHint : undefined}
              required
            />
              );
            })()}
            <Input
              label="Color"
              value={String(datos.color ?? '')}
              onChange={(e) => set('color', e.target.value)}
            />
          </div>
          <div className="grid grid-2">
            <Input
              label="Marca"
              value={String(datos.marca ?? '')}
              onChange={(e) => set('marca', e.target.value)}
              error={errs.marca}
              required
            />
            <Input
              label="Modelo"
              value={String(datos.modelo ?? '')}
              onChange={(e) => set('modelo', e.target.value)}
              error={errs.modelo}
              required
            />
          </div>
          <div className="grid grid-2">
            <Input
              label="Año"
              type="number"
              value={Number(datos.anio ?? 0)}
              onChange={(e) => set('anio', Number(e.target.value))}
              error={errs.anio}
              required
            />
            <Select
              label="Clasificación"
              value={String(datos.clasificacion ?? 'Gasolina')}
              onChange={(e) => set('clasificacion', e.target.value)}
              options={CLASIFICACIONES.map((c) => ({ value: c, label: c }))}
            />
          </div>

          {tipo === 'Auto' && (
            <div className="grid grid-2">
              <Input
                label="Nº de puertas"
                type="number"
                min={2}
                max={5}
                value={Number(datos.numeroPuertas ?? 4)}
                onChange={(e) => set('numeroPuertas', Number(e.target.value))}
                error={errs.numeroPuertas}
              />
              <Input
                label="Capacidad maletero (L)"
                type="number"
                value={Number(datos.capacidadMaletero ?? 0)}
                onChange={(e) => set('capacidadMaletero', Number(e.target.value))}
                error={errs.capacidadMaletero}
              />
            </div>
          )}
          {tipo === 'Motocicleta' && (
            <div className="grid grid-2">
              <Input
                label="Cilindraje (cc)"
                type="number"
                value={Number(datos.cilindraje ?? 0)}
                onChange={(e) => set('cilindraje', Number(e.target.value))}
                error={errs.cilindraje}
              />
              <Select
                label="Tipo de moto"
                value={String(datos.tipoMoto ?? 'Deportiva')}
                onChange={(e) => set('tipoMoto', e.target.value)}
                options={TIPOS_MOTO.map((t) => ({ value: t, label: t }))}
              />
            </div>
          )}
          {tipo === 'Camioneta' && (
            <div className="grid grid-2">
              <Select
                label="Cabina"
                value={String(datos.cabina ?? 4)}
                onChange={(e) => set('cabina', Number(e.target.value))}
                options={[
                  { value: '2', label: 'Simple (2)' },
                  { value: '4', label: 'Doble (4)' },
                ]}
              />
              <Input
                label="Capacidad de carga"
                value={String(datos.capacidadCarga ?? '')}
                onChange={(e) => set('capacidadCarga', e.target.value)}
                hint="Ej: 2.5t"
              />
            </div>
          )}
        </div>
      </Modal>
    </>
  );
}
