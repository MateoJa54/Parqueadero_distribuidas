import { Controller, Get, Post, Body, Param, UseGuards } from '@nestjs/common';
import { AuditService } from './audit.service';
import { CreateAuditEventDto } from './dto/create-audit.dto';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// La auditoria expone datos sensibles (ip, mac, usuario). Ademas del JWT valido
// (JwtAuthGuard global), se exige rol ADMIN o ROOT: un CLIENTE/RECAUDADOR no
// puede consultarla ni inyectar eventos. Los eventos reales entran por RabbitMQ.
@Controller('audit')
@UseGuards(RolesGuard)
@Roles('ADMIN', 'ROOT')
export class AuditController {
  constructor(private readonly auditService: AuditService) {}

  @Post()
  create(@Body() createAuditDto: CreateAuditEventDto) {
    return this.auditService.create(createAuditDto);
  }

  @Get()
  findAll() {
    return this.auditService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.auditService.findOne(id);
  }
}
