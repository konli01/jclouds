/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.slicehost.compute.config;

import static org.jclouds.compute.domain.OsFamily.UBUNTU;

import java.util.Map;

import javax.inject.Singleton;

import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.config.ComputeServiceAdapterContextModule;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.internal.BaseComputeService;
import org.jclouds.domain.Location;
import org.jclouds.functions.IdentityFunction;
import org.jclouds.location.suppliers.OnlyLocationOrFirstZone;
import org.jclouds.slicehost.SlicehostAsyncClient;
import org.jclouds.slicehost.SlicehostClient;
import org.jclouds.slicehost.compute.functions.FlavorToHardware;
import org.jclouds.slicehost.compute.functions.SliceToNodeMetadata;
import org.jclouds.slicehost.compute.functions.SlicehostImageToImage;
import org.jclouds.slicehost.compute.functions.SlicehostImageToOperatingSystem;
import org.jclouds.slicehost.compute.strategy.SlicehostComputeServiceAdapter;
import org.jclouds.slicehost.domain.Flavor;
import org.jclouds.slicehost.domain.Slice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

/**
 * Configures the {@link SlicehostComputeServiceContext}; requires {@link BaseComputeService} bound.
 * 
 * @author Adrian Cole
 */
public class SlicehostComputeServiceContextModule
         extends
         ComputeServiceAdapterContextModule<SlicehostClient, SlicehostAsyncClient, Slice, Flavor, org.jclouds.slicehost.domain.Image, Location> {
   public SlicehostComputeServiceContextModule() {
      super(SlicehostClient.class, SlicehostAsyncClient.class);
   }

   @SuppressWarnings("unchecked")
   @Override
   protected void configure() {
      super.configure();
      bind(new TypeLiteral<ComputeServiceAdapter<Slice, Flavor, org.jclouds.slicehost.domain.Image, Location>>() {
      }).to(SlicehostComputeServiceAdapter.class);

      bind(new TypeLiteral<Function<Slice, NodeMetadata>>() {
      }).to(SliceToNodeMetadata.class);

      bind(new TypeLiteral<Function<org.jclouds.slicehost.domain.Image, Image>>() {
      }).to(SlicehostImageToImage.class);
      bind(new TypeLiteral<Function<org.jclouds.slicehost.domain.Image, OperatingSystem>>() {
      }).to(SlicehostImageToOperatingSystem.class);

      bind(new TypeLiteral<Function<Flavor, Hardware>>() {
      }).to(FlavorToHardware.class);

      // we aren't converting location from a provider-specific type
      bind(new TypeLiteral<Function<Location, Location>>() {
      }).to((Class) IdentityFunction.class);

      // there are no locations except the provider
      bind(new TypeLiteral<Supplier<Location>>() {
      }).to(OnlyLocationOrFirstZone.class);

   }

   @Override
   protected TemplateBuilder provideTemplate(Injector injector, TemplateBuilder template) {
      return template.osFamily(UBUNTU).osVersionMatches("1[10].[10][04]").osDescriptionMatches("^((?!MGC).)*$")
               .os64Bit(true);
   }

   @VisibleForTesting
   public static final Map<Slice.Status, NodeState> sliceStatusToNodeState = ImmutableMap
            .<Slice.Status, NodeState> builder().put(Slice.Status.ACTIVE, NodeState.RUNNING)//
            .put(Slice.Status.BUILD, NodeState.PENDING)//
            .put(Slice.Status.REBOOT, NodeState.PENDING)//
            .put(Slice.Status.HARD_REBOOT, NodeState.PENDING)//
            .put(Slice.Status.TERMINATED, NodeState.TERMINATED)//
            .put(Slice.Status.UNRECOGNIZED, NodeState.UNRECOGNIZED)//
            .build();

   @Singleton
   @Provides
   Map<Slice.Status, NodeState> provideSliceToNodeState() {
      return sliceStatusToNodeState;
   }
}
