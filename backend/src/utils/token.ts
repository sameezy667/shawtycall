/**
 * @file token.ts
 * @description LiveKit JWT Access Token Generator
 * @module backend/utils
 */

import { AccessToken } from "livekit-server-sdk";

interface TokenGenerationParams {
  roomName: string;
  participantName: string;
}

/**
 * Generates a signed LiveKit JWT Access Token.
 * @param params Room name and participant name
 * @returns Object containing the generated token and configured server URL
 */
export async function generateLiveKitToken(params: TokenGenerationParams): Promise<{ token: string; serverUrl: string }> {
  const apiKey = Bun.env.LIVEKIT_API_KEY;
  const apiSecret = Bun.env.LIVEKIT_API_SECRET;
  const serverUrl = Bun.env.LIVEKIT_URL;

  if (!apiKey || !apiSecret || !serverUrl) {
    throw new Error("Missing LiveKit environment configuration parameters.");
  }

  // Create token with participant identity
  const at = new AccessToken(apiKey, apiSecret, {
    identity: params.participantName,
  });

  // Grant publish, subscribe, and join permissions for the target room
  at.addGrant({
    roomJoin: true,
    room: params.roomName,
    canPublish: true,
    canSubscribe: true,
  });

  const token = await at.toJwt();

  return {
    token,
    serverUrl,
  };
}
