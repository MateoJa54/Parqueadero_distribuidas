import { Controller, Get, Post, Body, Patch, Param, Query } from '@nestjs/common';
import { VehiculosService } from './vehiculos.service';
import { CreateVehiculoDto } from './dto/create-vehiculo.dto';
import { UpdateVehiculoDto } from './dto/update-vehiculo.dto';
import { Roles } from '../auth/roles.decorator';

@Controller('vehiculos')
export class VehiculosController {
  constructor(private readonly vehiculosService: VehiculosService) {}

  @Post()
  @Roles('ADMIN', 'ROOT')
  create(@Body() createVehiculoDto: CreateVehiculoDto) {
    return this.vehiculosService.create(createVehiculoDto);
  }

  @Get()
  findAll(@Query('incluirInactivos') incluirInactivos?: string) {
    return this.vehiculosService.findAll(incluirInactivos === 'true');
  }

  // Ruta literal antes de ':id' para que no sea capturada como id.
  @Get('placa/:placa')
  findByPlaca(@Param('placa') placa: string) {
    return this.vehiculosService.findByPlaca(placa);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.vehiculosService.findOne(id);
  }

  @Patch(':id')
  @Roles('ADMIN', 'ROOT')
  update(@Param('id') id: string, @Body() updateVehiculoDto: UpdateVehiculoDto) {
    return this.vehiculosService.update(id, updateVehiculoDto);
  }

  @Patch(':id/activar')
  @Roles('ADMIN', 'ROOT')
  activar(@Param('id') id: string) {
    return this.vehiculosService.activar(id);
  }

  @Patch(':id/desactivar')
  @Roles('ADMIN', 'ROOT')
  desactivar(@Param('id') id: string) {
    return this.vehiculosService.desactivar(id);
  }
}
