import { VehiculosModule } from './vehiculos.module';
import { VehiculosService } from './vehiculos.service';
import { VehiculosController } from './vehiculos.controller';

describe('VehiculosModule', () => {
  it('se define y referencia sus componentes principales', () => {
    expect(VehiculosModule).toBeDefined();
    expect(new VehiculosModule()).toBeInstanceOf(VehiculosModule);
    expect(VehiculosService).toBeDefined();
    expect(VehiculosController).toBeDefined();
  });
});
