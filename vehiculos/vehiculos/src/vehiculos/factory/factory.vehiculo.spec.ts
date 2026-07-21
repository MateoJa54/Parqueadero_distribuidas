import { FactoryVehiculos } from './factory.vehiculo';
import { Auto } from '../entities/auto.entity';
import { Motocicleta } from '../entities/motocicleta.entity';
import { Camioneta } from '../entities/camioneta.entity';
import { CreateVehiculoDto } from '../dto/create-vehiculo.dto';

describe('FactoryVehiculos', () => {
  it('crea un Auto y copia los datos', () => {
    const dto = { tipo: 'Auto', datos: { placa: 'ABC-1234' } } as unknown as CreateVehiculoDto;
    const result = FactoryVehiculos.crear(dto);
    expect(result).toBeInstanceOf(Auto);
    expect(result.placa).toBe('ABC-1234');
  });

  it('crea una Motocicleta', () => {
    const dto = { tipo: 'Motocicleta', datos: { placa: 'M-1' } } as unknown as CreateVehiculoDto;
    expect(FactoryVehiculos.crear(dto)).toBeInstanceOf(Motocicleta);
  });

  it('crea una Camioneta', () => {
    const dto = { tipo: 'Camioneta', datos: { placa: 'C-1' } } as unknown as CreateVehiculoDto;
    expect(FactoryVehiculos.crear(dto)).toBeInstanceOf(Camioneta);
  });

  it('lanza error para un tipo no soportado', () => {
    const dto = { tipo: 'Avion', datos: {} } as unknown as CreateVehiculoDto;
    expect(() => FactoryVehiculos.crear(dto)).toThrow('Tipo de vehículo no soportado: Avion');
  });
});
