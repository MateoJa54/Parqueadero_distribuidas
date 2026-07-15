import { Controller, Get, Post, Body, Patch, Param, Query, Req } from '@nestjs/common';
import type { Request } from 'express';
import { VehiculosService, AuditContext } from './vehiculos.service';
import { CreateVehiculoDto } from './dto/create-vehiculo.dto';
import { UpdateVehiculoDto } from './dto/update-vehiculo.dto';
import { Roles } from '../auth/roles.decorator';
import { JwtPayload } from '../auth/jwt-auth.guard';

const MAC_POR_DEFECTO = '00:00:00:00:00:00';

// Arma el contexto de auditoría (quién, desde dónde) a partir del JWT ya
// validado por JwtAuthGuard (request.user) y del header que el dispositivo
// cliente (kiosko/frontend) debe enviar con su MAC.
function normalizarIp(ip: string): string {
  // ms-audit valida IPv4 estricto; en local es comun recibir formas IPv6
  // de loopback ("::1") o IPv4-mapped-IPv6 ("::ffff:127.0.0.1").
  if (ip === '::1') return '127.0.0.1';
  return ip.replace(/^::ffff:/, '');
}

// Kong (y cualquier proxy delante del servicio) agrega "X-Forwarded-For" con
// la IP real del cliente antes de reenviar la petición; sin esto, req.ip solo
// vería la IP de Kong, no la del usuario final. Si trae varias IPs separadas
// por coma (cadena de proxies), la primera es la del cliente original.
//
// ADVERTENCIA: cualquiera que llame directo al microservicio (sin pasar por
// Kong) puede mandar este header con lo que quiera, así que no es una fuente
// 100% confiable si el puerto del servicio queda expuesto sin el gateway
// delante. Sirve como dato informativo, no como prueba forense.
function ipOrigenDeLaPeticion(req: Request): string {
  const forwardedFor = req.headers['x-forwarded-for'];
  const valor = Array.isArray(forwardedFor) ? forwardedFor[0] : forwardedFor;
  if (valor) {
    return valor.split(',')[0].trim();
  }
  return req.ip ?? req.socket.remoteAddress ?? '';
}

function auditContextDesdeRequest(req: Request): AuditContext {
  const user = req.user as JwtPayload | undefined;
  const ip = normalizarIp(ipOrigenDeLaPeticion(req));
  const mac = (req.headers['x-device-mac'] as string) ?? MAC_POR_DEFECTO;

  return {
    usuario: user?.username,
    rol: user?.roles?.[0],
    ip: ip || '127.0.0.1',
    mac,
  };
}

@Controller('vehiculos')
export class VehiculosController {
  constructor(private readonly vehiculosService: VehiculosService) {}

  @Post()
  @Roles('ADMIN', 'ROOT')
  create(@Body() createVehiculoDto: CreateVehiculoDto, @Req() req: Request) {
    return this.vehiculosService.create(createVehiculoDto, auditContextDesdeRequest(req));
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
  update(
    @Param('id') id: string,
    @Body() updateVehiculoDto: UpdateVehiculoDto,
    @Req() req: Request,
  ) {
    return this.vehiculosService.update(id, updateVehiculoDto, auditContextDesdeRequest(req));
  }

  @Patch(':id/activar')
  @Roles('ADMIN', 'ROOT')
  activar(@Param('id') id: string, @Req() req: Request) {
    return this.vehiculosService.activar(id, auditContextDesdeRequest(req));
  }

  @Patch(':id/desactivar')
  @Roles('ADMIN', 'ROOT')
  desactivar(@Param('id') id: string, @Req() req: Request) {
    return this.vehiculosService.desactivar(id, auditContextDesdeRequest(req));
  }
}
