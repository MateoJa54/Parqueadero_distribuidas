import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import {
  AutoDto,
  CamionetaDto,
  CreateVehiculoDto,
  MotocicletaDto,
} from './create-vehiculo.dto';
import { UpdateVehiculoDto } from './update-vehiculo.dto';
import { Clasificacion } from '../entities/vehiculo.entity';
import { TipoMoto } from '../entities/motocicleta.entity';
import { ResponseVehiculoDto } from './response-vehiculo.dto';

const baseValido = {
  placa: 'ABC-1234',
  marca: 'Toyota',
  modelo: 'Corolla',
  color: 'Azul',
  anio: 2024,
  clasificacion: Clasificacion.GASOLINA,
};

describe('CreateVehiculoDto (discriminador polimorfico)', () => {
  it('transforma datos a AutoDto cuando tipo=Auto y valida', async () => {
    const dto = plainToInstance(CreateVehiculoDto, {
      tipo: 'Auto',
      datos: { ...baseValido, numeroPuertas: 4, capacidadMaletero: 470 },
    });
    expect(dto.datos).toBeInstanceOf(AutoDto);
    expect(await validate(dto)).toHaveLength(0);
  });

  it('transforma datos a MotocicletaDto cuando tipo=Motocicleta', async () => {
    const dto = plainToInstance(CreateVehiculoDto, {
      tipo: 'Motocicleta',
      datos: {
        ...baseValido,
        placa: 'AB-123A',
        cilindraje: 250,
        tipoMoto: TipoMoto.DEPORTIVA,
      },
    });
    expect(dto.datos).toBeInstanceOf(MotocicletaDto);
    expect(await validate(dto)).toHaveLength(0);
  });

  it('transforma datos a CamionetaDto cuando tipo=Camioneta', async () => {
    const dto = plainToInstance(CreateVehiculoDto, {
      tipo: 'Camioneta',
      datos: { ...baseValido, cabina: 4, capacidadCarga: '750kg' },
    });
    expect(dto.datos).toBeInstanceOf(CamionetaDto);
    expect(await validate(dto)).toHaveLength(0);
  });

  it('usa BaseVehiculoDto cuando el tipo es desconocido', () => {
    const dto = plainToInstance(CreateVehiculoDto, {
      tipo: 'Avion',
      datos: { ...baseValido },
    });
    expect(dto.datos).toBeDefined();
  });

  it('tolera un objeto sin tipo definido (rama default)', () => {
    const dto = plainToInstance(CreateVehiculoDto, {
      datos: { ...baseValido },
    });
    expect(dto.datos).toBeDefined();
  });

  it('reporta errores cuando los datos no cumplen las reglas', async () => {
    const dto = plainToInstance(CreateVehiculoDto, {
      tipo: 'Auto',
      datos: { ...baseValido, placa: 'mala', numeroPuertas: 9, capacidadMaletero: 470 },
    });
    const errores = await validate(dto);
    expect(errores.length).toBeGreaterThan(0);
  });
});

describe('UpdateVehiculoDto (discriminador parcial)', () => {
  it('permite un patch parcial de Auto', async () => {
    const dto = plainToInstance(UpdateVehiculoDto, {
      tipo: 'Auto',
      datos: { numeroPuertas: 2 },
    });
    expect(await validate(dto)).toHaveLength(0);
  });

  it('permite un patch parcial de Motocicleta', async () => {
    const dto = plainToInstance(UpdateVehiculoDto, {
      tipo: 'Motocicleta',
      datos: { cilindraje: 150 },
    });
    expect(await validate(dto)).toHaveLength(0);
  });

  it('permite un patch parcial de Camioneta', async () => {
    const dto = plainToInstance(UpdateVehiculoDto, {
      tipo: 'Camioneta',
      datos: { cabina: 2 },
    });
    expect(await validate(dto)).toHaveLength(0);
  });

  it('usa la variante base cuando se omite el tipo', async () => {
    const dto = plainToInstance(UpdateVehiculoDto, {
      datos: { color: 'Rojo' },
    });
    expect(await validate(dto)).toHaveLength(0);
  });

  it('cae en la rama default con un tipo desconocido', () => {
    const dto = plainToInstance(UpdateVehiculoDto, {
      tipo: 'Avion',
      datos: { color: 'Rojo' },
    });
    expect(dto.datos).toBeDefined();
  });
});

describe('ResponseVehiculoDto', () => {
  it('se instancia como contenedor de datos de respuesta', () => {
    const dto = new ResponseVehiculoDto();
    dto.id = 'v1';
    dto.tipo = 'Auto';
    expect(dto.id).toBe('v1');
    expect(dto.tipo).toBe('Auto');
  });
});
