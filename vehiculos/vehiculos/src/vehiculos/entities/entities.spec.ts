import { Auto } from './auto.entity';
import { Camioneta } from './camioneta.entity';
import { Motocicleta, TipoMoto } from './motocicleta.entity';
import { Clasificacion } from './vehiculo.entity';

describe('Entidades de vehiculo', () => {
  it('Auto expone su tipo tras los hooks del ciclo de vida', () => {
    const auto = new Auto();
    expect(auto.obtenerTipo()).toBe('Auto');
    // exponerTipo es protected pero se invoca via los hooks; lo forzamos.
    (auto as unknown as { exponerTipo: () => void }).exponerTipo();
    expect(auto.tipo).toBe('Auto');
  });

  it('Camioneta expone su tipo', () => {
    const camioneta = new Camioneta();
    expect(camioneta.obtenerTipo()).toBe('Camioneta');
    (camioneta as unknown as { exponerTipo: () => void }).exponerTipo();
    expect(camioneta.tipo).toBe('Camioneta');
  });

  it('Motocicleta expone su tipo', () => {
    const moto = new Motocicleta();
    moto.tipoMoto = TipoMoto.DEPORTIVA;
    expect(moto.obtenerTipo()).toBe('Motocicleta');
    (moto as unknown as { exponerTipo: () => void }).exponerTipo();
    expect(moto.tipo).toBe('Motocicleta');
  });

  it('los enums exponen sus valores de negocio', () => {
    expect(Clasificacion.GASOLINA).toBe('Gasolina');
    expect(TipoMoto.SCOOTER).toBe('Scooter');
  });
});
